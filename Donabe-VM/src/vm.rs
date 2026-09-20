use crate::builtin_functions;
use crate::builtin_functions::dispatch_builtin_function;
use crate::bytecode::ByteCode;
use crate::bytecode::section::ConstantPoolEntry;
use crate::heap::handle::Handle;
use crate::heap::heap::Heap;
use crate::instruction::OpCode;
use crate::stack_frame::{FrameRef, StackFrame};
use crate::stack_frame_cache::StackFrameCache;
use crate::value::{BuiltinFunctionKind, Value, ValueRef};
use std::cell::{Ref, RefCell, RefMut};
use std::collections::HashMap;
use std::fmt::Display;
use std::rc::Rc;

#[macro_export]
macro_rules! error_with_pc {
    ($pc:expr, $($arg:tt)*) => {
        RuntimeError::new($pc, format!($($arg)*))
    };
}

#[macro_export]
macro_rules! error_without_pc {
    ($($arg:tt)*) => {
        crate::vm::RuntimeError::new_without_pc(format!($($arg)*))
    };
}

#[macro_export]
macro_rules! error {
    ($self:expr, $($arg:tt)*) => {
        RuntimeError::new($self.borrow_current_frame().pc(), format!($($arg)*))
    };
}

#[macro_export]
macro_rules! bail {
    ($($args:tt)*) => {
        return Err(error!($($args)*))
    };
}

struct VMContext {
    call_stack: Vec<FrameRef>,
    stack_frame_cache: StackFrameCache,
}

enum VMConstantEntry {
    MemberRef { member_name: String },
    Handle(Handle),
}

pub struct VM {
    constant_pool: Vec<VMConstantEntry>,
    receiver_table: Vec<ValueRef>,
    context: VMContext,
    heap: Heap,
    undefined_ref: ValueRef,
}

#[derive(Debug)]
pub struct RuntimeError(Option<u32>, String);

impl RuntimeError {
    pub fn new(pc: u32, msg: impl Into<String>) -> Self {
        Self(Some(pc), msg.into())
    }

    pub fn new_without_pc(msg: impl Into<String>) -> Self {
        Self(None, msg.into())
    }
}

impl Display for RuntimeError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        if let Some(pc) = self.0.as_ref() {
            write!(f, "{} at PC {}", self.1, pc)
        } else {
            write!(f, "{}", self.1)
        }
    }
}

impl VM {
    pub fn new(byte_code: &ByteCode) -> Result<VM, RuntimeError> {
        let mut heap =
            Heap::new().map_err(|e| error_without_pc!("Could not allocation heap: {}", e))?;
        let undefined_handle = heap
            .alloc(Value::Undefined)
            .map_err(|_| error_without_pc!("Could not allocate undefined value"))?;

        let mut constant_pool = Vec::new();
        for v in byte_code.constant_pool.values() {
            constant_pool.push(match v {
                ConstantPoolEntry::MemberRef { member_name } => {
                    VMConstantEntry::MemberRef { member_name }
                }

                ConstantPoolEntry::Value { value } => VMConstantEntry::Handle(
                    heap.alloc(value).map_err(|e| error_without_pc!("{}", e))?,
                ),
            });
        }

        let identifier_slots_set = byte_code.identifiers.slots();
        let mut identifier_slots = identifier_slots_set
            .iter()
            .enumerate()
            .map(|(_, slot)| (*slot, undefined_handle))
            .collect();
        setup_builtin_functions(&mut heap, &mut identifier_slots)?;

        let root_frame = Rc::new(RefCell::new(StackFrame::new(
            "<root>".to_string(),
            None,
            Rc::new(byte_code.code_section.code()),
            0,
            identifier_slots,
        )));

        Ok(VM {
            constant_pool,
            receiver_table: Vec::new(),
            context: VMContext {
                call_stack: vec![root_frame.clone()],
                stack_frame_cache: StackFrameCache::new(root_frame),
            },
            heap,
            undefined_ref: undefined_handle,
        })
    }

    /// VMの実行を開始する。
    pub fn run(&mut self) -> Result<(), RuntimeError> {
        while self.borrow_current_frame().pc() < self.borrow_current_frame().code().len() as u32 {
            // println!("[Log] Current stack: {:?}, SP={}", self.context.operand_stack, self.get_current_frame()?.borrow().sp());
            // println!("[Log] Current stack base: {:?}", self.get_current_frame()?.borrow().stack_base());

            let opcode = self.fetch_opcode()?;
            let operand_size = opcode.get_operand_size();

            let operand_candidate = self.borrow_current_frame().pc() + 0x01;

            self.borrow_current_frame_mut()
                .increase_pc(0x01 + operand_size);

            self.execute(operand_candidate, opcode)?;
        }
        Ok(())
    }

    fn execute(&mut self, operand_candidate: u32, opcode: OpCode) -> Result<(), RuntimeError> {
        match opcode {
            OpCode::Push => {
                let constant_pool_value = &self.constant_pool
                    [operand_2bytes(self.get_code()?, operand_candidate)? as usize];
                match constant_pool_value {
                    VMConstantEntry::MemberRef { .. } => {
                        bail!(self, "MemberRef cannot be pushed.")
                    }
                    VMConstantEntry::Handle(handle) => {
                        match self.get_value(*handle)? {
                            Value::Function { .. } => {
                                let closure = Value::Closure {
                                    function_handle: *handle,
                                    parent: self.get_current_frame(),
                                };
                                self.push_stack_alloc(closure)?;
                            }
                            _ => {
                                self.push_stack(*handle)?;
                            }
                        };
                        Ok(())
                    }
                }?
            }
            OpCode::Pop => {
                self.pop_stack()?;
            }
            OpCode::Add => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::add(self.get_value(lhs)?, self.get_value(rhs)?)?)?;
            }
            OpCode::Sub => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::sub(self.get_value(lhs)?, self.get_value(rhs)?)?)?;
            }
            OpCode::Mul => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::mul(self.get_value(lhs)?, self.get_value(rhs)?)?)?;
            }
            OpCode::Div => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::div(self.get_value(lhs)?, self.get_value(rhs)?)?)?;
            }
            OpCode::Equal => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::equal(self.get_value(lhs)?, self.get_value(rhs)?)?)?;
            }
            OpCode::Greater => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::greater(self.get_value(lhs)?, self.get_value(rhs)?)?)?;
            }
            OpCode::GreaterEqual => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::greater_equal(
                    self.get_value(lhs)?,
                    self.get_value(rhs)?,
                )?)?;
            }
            OpCode::Less => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::less(self.get_value(lhs)?, self.get_value(rhs)?)?)?;
            }
            OpCode::LessEqual => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack_alloc(Value::less_equal(
                    self.get_value(lhs)?,
                    self.get_value(rhs)?,
                )?)?;
            }
            OpCode::Call => {
                let undefined_ref = self.undefined_ref;
                let target = self.pop_stack_get()?;

                match target.clone() {
                    Value::Closure {
                        function_handle,
                        parent,
                    } => {
                        let (name, params, locals, code) = {
                            let function = self.get_value(function_handle)?;

                            if let Value::Function {
                                name,
                                params,
                                locals,
                                code,
                            } = function
                            {
                                (
                                    name.clone(),
                                    params.clone(),
                                    locals.clone(),
                                    Rc::clone(code),
                                )
                            } else {
                                bail!(self, "Invalid function handle.");
                            }
                        };

                        let mut local_vars: HashMap<u16, ValueRef> = locals
                            .iter()
                            .map(|slot| (*slot, undefined_ref.clone()))
                            .collect();

                        for param in &params {
                            let param_value = self.pop_stack()?;

                            if !local_vars.contains_key(param) {
                                bail!(
                                    self,
                                    "Unbindable argument: {}. Non-existent in locals.",
                                    param
                                );
                            }

                            local_vars.insert(*param, param_value);
                        }

                        let callee_frame = StackFrame::new(
                            name,
                            Some(parent.clone()),
                            code,
                            self.borrow_current_frame().sp(),
                            local_vars,
                        );

                        self.context
                            .call_stack
                            .push(Rc::new(RefCell::new(callee_frame)));

                        self.update_frame_cache()?;
                        return Ok(());
                    }
                    Value::BuiltinFunction {
                        param_count,
                        body,
                        receiver_id,
                        ..
                    } => {
                        let mut param_bindings = Vec::new();

                        for _ in 0u32..param_count {
                            match self.pop_stack() {
                                Ok(v) => {
                                    param_bindings.push(v.clone());
                                }
                                Err(e) => bail!(self, "Insufficient arguments."),
                            }
                        }

                        let receiver = match receiver_id {
                            None => None,
                            Some(v) => Some(self.receiver_table[v].clone()),
                        };

                        let ret_value = dispatch_builtin_function(
                            body.clone(),
                            param_bindings,
                            receiver,
                            &self.heap,
                        )?;
                        self.push_stack_alloc(ret_value)?;
                        return Ok(());
                    }
                    _ => {},
                }
                bail!(self, "Invalid callee.");
            }
            OpCode::Index => {
                let index_value = self.pop_stack()?;
                let target = self.pop_stack()?;
                match (
                    self.get_value(index_value)?,
                    self.get_value(target.clone())?,
                ) {
                    (Value::Int { value: index }, Value::List { value: list }) => {
                        if *index < 0 || *index >= list.len() as i32 {
                            bail!(
                                self,
                                "Index {} out of bounds for length {}",
                                *index,
                                list.len()
                            );
                        }
                        self.push_stack_alloc(list[*index as usize].clone())?;
                    }
                    (_, _) => bail!(
                        self,
                        "Only List values can be indexed: {}",
                        self.get_value(target)?
                    ),
                };
            }
            OpCode::Jmp => {
                let jmp_to = operand_4bytes(self.get_code()?, operand_candidate)?;
                self.borrow_current_frame_mut().set_pc(jmp_to);
            }
            OpCode::JmpFalse => {
                let jmp_to = operand_4bytes(self.get_code()?, operand_candidate)?;
                let pc = self.borrow_current_frame().pc();
                let condition = self
                    .pop_stack_get()?
                    .expect_bool()
                    .map_err(|_| error_with_pc!(pc, "Condition must be type of Bool."))?;
                if !condition {
                    self.borrow_current_frame_mut().set_pc(jmp_to);
                }
            }
            OpCode::JmpTrue => {
                let jmp_to = operand_4bytes(self.get_code()?, operand_candidate)?;
                let pc = self.borrow_current_frame().pc();
                let condition = self
                    .pop_stack_get()?
                    .expect_bool()
                    .map_err(|_| error_with_pc!(pc, "Condition must be type of Bool."))?;
                if condition {
                    self.borrow_current_frame_mut().set_pc(jmp_to);
                }
            }
            OpCode::Nop => {
                //何もしない
            }
            OpCode::LoadCaptured => {
                let slot = operand_2bytes(self.get_code()?, operand_candidate)?;
                let value_ref = match self.borrow_current_frame().get_captured_var(slot) {
                    None => bail!(self, "Non-existent slot: {}", slot),
                    Some(v) => v,
                };
                self.push_stack(value_ref)?;
            }
            OpCode::LoadLocal => {
                let slot = operand_2bytes(self.get_code()?, operand_candidate)?;
                let value_ref = match self.borrow_current_frame().get_local_var(slot) {
                    None => bail!(self, "Non-existent slot: {}", slot),
                    Some(v) => v,
                };
                self.push_stack(value_ref)?;
            }
            OpCode::LoadMember => {
                let receiver_ref = self.pop_stack()?;
                let entry = &self.constant_pool
                    [operand_2bytes(self.get_code()?, operand_candidate)? as usize];

                match entry {
                    VMConstantEntry::MemberRef { member_name } => {
                        let receiver_id = self.receiver_table.len();
                        self.receiver_table.push(receiver_ref.clone());
                        let receiver = self.get_value(receiver_ref)?;

                        match receiver.find_member(&member_name, receiver_id) {
                            None => bail!(self, "Non-existent member: {}", member_name),
                            Some(v) => {
                                self.push_stack_alloc(v)?;
                            }
                        }
                    }
                    VMConstantEntry::Handle(_) => {
                        bail!(self, "Value cannot be used as MemberRef.")
                    }
                };
            }
            OpCode::StoreCaptured => {
                let slot = operand_2bytes(self.get_code()?, operand_candidate)?;
                let value_ref = self.pop_stack()?;

                self.borrow_current_frame_mut()
                    .set_captured_var(slot, value_ref.clone())?;
            }
            OpCode::StoreLocal => {
                let slot = operand_2bytes(self.get_code()?, operand_candidate)?;
                let value_ref = self.pop_stack()?;

                self.borrow_current_frame_mut()
                    .set_local_var(slot, value_ref.clone())?;
            }
            OpCode::MakeList => {
                let item_count = operand_4bytes(self.get_code()?, operand_candidate)?;
                let mut items = Vec::with_capacity(item_count as usize);

                for _ in 0..item_count {
                    let item = self.pop_stack_get()?;
                    items.push(item.clone());
                }
                items.reverse();
                self.push_stack_alloc(Value::List { value: items })?;
            }
            OpCode::Not => {
                let target = self.pop_stack()?;
                self.push_stack_alloc(Value::not(self.get_value(target)?)?)?;
            }
            OpCode::Plus => {
                let target = self.pop_stack()?;
                self.push_stack_alloc(Value::plus(self.get_value(target)?)?)?;
            }
            OpCode::Minus => {
                let target = self.pop_stack()?;
                self.push_stack_alloc(Value::minus(self.get_value(target)?)?)?;
            }
            OpCode::Return => {
                let return_value = self.pop_stack()?;
                self.context.call_stack.pop();

                self.update_frame_cache()?;

                self.push_stack(return_value)?;
            }
            OpCode::VReturn => {
                self.context.call_stack.pop();

                self.update_frame_cache()?;

                self.push_stack_alloc(Value::Void)?;
            }
        };
        Ok(())
    }

    fn get_code(&self) -> Result<Rc<Vec<u8>>, RuntimeError> {
        Ok(self.borrow_current_frame().code())
    }

    fn fetch_opcode(&self) -> Result<OpCode, RuntimeError> {
        let current_frame = self.borrow_current_frame();
        let opcode_byte = current_frame.code()[current_frame.pc() as usize];
        match OpCode::from(opcode_byte) {
            Some(v) => Ok(v),
            None => Err(error!(self, "Invalid opcode: {}", opcode_byte)),
        }
    }

    fn push_stack(&mut self, value: ValueRef) -> Result<(), RuntimeError> {
        Ok(self
            .get_current_frame()
            .borrow_mut()
            .push_operand_stack(value))
    }

    fn push_stack_alloc(&mut self, value: Value) -> Result<(), RuntimeError> {
        Ok(self
            .get_current_frame()
            .borrow_mut()
            .push_operand_stack(self.alloc(value)?))
    }

    fn pop_stack_get(&mut self) -> Result<&Value, RuntimeError> {
        let value_ref = self.get_current_frame().borrow_mut().pop_operand_stack()?;

        Ok(self.get_value(value_ref)?)
    }

    fn pop_stack(&mut self) -> Result<ValueRef, RuntimeError> {
        self.get_current_frame().borrow_mut().pop_operand_stack()
    }

    fn get_current_frame(&self) -> FrameRef {
        self.context.stack_frame_cache.current_frame()
    }

    fn borrow_current_frame(&self) -> Ref<'_, StackFrame> {
        self.context.stack_frame_cache.borrow_frame_ref()
    }

    fn borrow_current_frame_mut(&self) -> RefMut<'_, StackFrame> {
        self.context.stack_frame_cache.borrow_frame_ref_mut()
    }

    fn update_frame_cache(&mut self) -> Result<(), RuntimeError> {
        Ok(self.context.stack_frame_cache.update_frame(
            self.context
                .call_stack
                .last()
                .ok_or_else(|| error_without_pc!("Could not update stack frame."))?
                .clone(),
        ))
    }

    fn alloc(&mut self, value: Value) -> Result<ValueRef, RuntimeError> {
        self.heap
            .alloc(value)
            .map_err(|e| error!(self, "Could not allocate: {}", e))
    }

    fn get_value(&self, value_ref: ValueRef) -> Result<&Value, RuntimeError> {
        self.heap
            .get(value_ref)
            .map_err(|e| error!(self, "Could not get value: {}", e))
    }

    fn get_value_mut(&mut self, value_ref: ValueRef) -> Result<&mut Value, RuntimeError> {
        self.heap
            .get_mut(value_ref)
            .map_err(|e| error!(self, "Could not get value: {}", e))
    }
}

fn operand_2bytes(code: Rc<Vec<u8>>, pos: u32) -> Result<u16, RuntimeError> {
    Ok(u16::from_le_bytes(
        code[pos as usize..pos as usize + 2]
            .try_into()
            .map_err(|e| error_with_pc!(pos, "Invalid 2bytes operand: {}", e))?,
    ))
}

fn operand_4bytes(code: Rc<Vec<u8>>, pos: u32) -> Result<u32, RuntimeError> {
    Ok(u32::from_le_bytes(
        code[pos as usize..pos as usize + 4]
            .try_into()
            .map_err(|e| error_with_pc!(pos, "Invalid 4bytes operand: {}", e))?,
    ))
}

///引数のHashMapに組み込み関数を定義する。
fn setup_builtin_functions(
    heap: &mut Heap,
    identifier_slot: &mut HashMap<u16, ValueRef>,
) -> Result<(), RuntimeError> {
    identifier_slot.insert(
        0,
        heap.alloc(Value::BuiltinFunction {
            name: "print".to_string(),
            param_count: 1,
            receiver_id: None,
            body: BuiltinFunctionKind::Print,
        })
        .map_err(|e| error_without_pc!("Could not allocate 'print': {}", e))?,
    );
    identifier_slot.insert(
        1,
        heap.alloc(Value::BuiltinFunction {
            name: "input".to_string(),
            param_count: 0,
            receiver_id: None,
            body: BuiltinFunctionKind::Input,
        })
        .map_err(|e| error_without_pc!("Could not allocate 'input': {}", e))?,
    );
    identifier_slot.insert(
        2,
        heap.alloc(Value::BuiltinFunction {
            name: "range".to_string(),
            param_count: 2,
            receiver_id: None,
            body: BuiltinFunctionKind::Range,
        })
        .map_err(|e| error_without_pc!("Could not allocate 'range': {}", e))?,
    );
    identifier_slot.insert(
        3,
        heap.alloc(Value::BuiltinFunction {
            name: "now".to_string(),
            param_count: 0,
            receiver_id: None,
            body: BuiltinFunctionKind::Now,
        })
        .map_err(|e| error_without_pc!("Could not allocate 'now': {}", e))?,
    );
    Ok(())
}

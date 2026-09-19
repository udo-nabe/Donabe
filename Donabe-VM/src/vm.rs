use crate::builtin_functions;
use crate::builtin_functions::dispatch_builtin_function;
use crate::bytecode::ByteCode;
use crate::bytecode::section::ConstantPoolEntry;
use crate::instruction::OpCode;
use crate::stack_frame::{FrameRef, StackFrame};
use crate::value::{BuiltinFunctionKind, Value, ValueRef};
use std::cell::{Ref, RefCell, RefMut};
use std::collections::HashMap;
use std::fmt::Display;
use std::ops::Deref;
use std::rc::Rc;
use crate::stack_frame_cache::StackFrameCache;

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

pub struct VM {
    constant_pool: Vec<ConstantPoolEntry>,
    receiver_table: Vec<ValueRef>,
    context: VMContext,
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
    pub fn new(byte_code: &ByteCode) -> Self {
        let identifier_slots_set = byte_code.identifiers.slots();
        let mut identifier_slots = identifier_slots_set
            .iter()
            .enumerate()
            .map(|(_, slot)| (*slot, ValueRef::new(Value::Undefined)))
            .collect();
        setup_builtin_functions(&mut identifier_slots);

        let root_frame = Rc::new(RefCell::new(StackFrame::new(
            "<root>".to_string(),
            None,
            Rc::new(byte_code.code_section.code()),
            0,
            identifier_slots,
        )));

        VM {
            constant_pool: byte_code.constant_pool.values(),
            receiver_table: Vec::new(),
            context: VMContext {
                call_stack: vec![root_frame.clone()],
                stack_frame_cache: StackFrameCache::new(root_frame),
            },
        }
    }

    /// VMの実行を開始する。
    pub fn run(&mut self) -> Result<(), RuntimeError> {
        while self.borrow_current_frame().pc()
            < self.borrow_current_frame().code().len() as u32
        {
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
                let constant_pool_value = self.constant_pool
                    [operand_2bytes(self.get_code()?, operand_candidate)? as usize]
                    .clone();
                match constant_pool_value {
                    ConstantPoolEntry::MemberRef { .. } => {
                        bail!(self, "MemberRef cannot be pushed.")
                    }
                    ConstantPoolEntry::Value { value } => {
                        match value {
                            Value::Function {
                                name,
                                params,
                                locals,
                                code,
                            } => {
                                let closure = Value::Closure {
                                    name,
                                    params,
                                    locals,
                                    code,
                                    parent: self.get_current_frame(),
                                };
                                self.push_stack(ValueRef::new(closure))?;
                            }
                            _ => {
                                self.push_stack(ValueRef::new(value))?;
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
                self.push_stack(ValueRef::new(Value::add(&lhs.value(), &rhs.value())?))?;
            }
            OpCode::Sub => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack(ValueRef::new(Value::sub(&lhs.value(), &rhs.value())?))?;
            }
            OpCode::Mul => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack(ValueRef::new(Value::mul(&lhs.value(), &rhs.value())?))?;
            }
            OpCode::Div => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack(ValueRef::new(Value::div(&lhs.value(), &rhs.value())?))?;
            }
            OpCode::Equal => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack(ValueRef::new(Value::equal(&lhs.value(), &rhs.value())?))?;
            }
            OpCode::Greater => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack(ValueRef::new(Value::greater(&lhs.value(), &rhs.value())?))?;
            }
            OpCode::GreaterEqual => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack(ValueRef::new(Value::greater_equal(
                    &lhs.value(),
                    &rhs.value(),
                )?))?;
            }
            OpCode::Less => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack(ValueRef::new(Value::less(&lhs.value(), &rhs.value())?))?;
            }
            OpCode::LessEqual => {
                let rhs = self.pop_stack()?;
                let lhs = self.pop_stack()?;
                self.push_stack(ValueRef::new(Value::less_equal(
                    &lhs.value(),
                    &rhs.value(),
                )?))?;
            }
            OpCode::Call => {
                let target = self.pop_stack()?;
                match target.value().deref() {
                    Value::Closure {
                        name,
                        params,
                        locals,
                        code,
                        parent,
                    } => {
                        let mut local_vars: HashMap<u16, ValueRef> = locals
                            .iter()
                            .enumerate()
                            .map(|(_, v)| (*v, ValueRef::new(Value::Undefined)))
                            .collect();

                        for param in params {
                            let param_value = self.pop_stack()?;
                            if !local_vars.contains_key(param) {
                                bail!(self, "Unbindable argument: {}. Non-existent in locals.", param);
                            }
                            local_vars.insert(*param, param_value);
                        }

                        let callee_frame = StackFrame::new(
                            name.clone(),
                            Some(parent.clone()),
                            code.clone(),
                            self.borrow_current_frame().sp(),
                            local_vars,
                        );

                        self.context.call_stack.push(Rc::new(RefCell::new(callee_frame)));

                        self.update_frame_cache()?;
                    }
                    Value::BuiltinFunction {
                        param_count,
                        body,
                        receiver_id,
                        ..
                    } => {
                        let mut param_bindings = Vec::new();

                        for _ in 0u32..*param_count {
                            match self.pop_stack() {
                                Ok(v) => {
                                    param_bindings.push(v.clone());
                                }
                                Err(e) => bail!(self, "Insufficient arguments."),
                            }
                        }

                        let receiver = match receiver_id {
                            None => None,
                            Some(v) => Some(self.receiver_table[*v].clone()),
                        };

                        let ret_value =
                            dispatch_builtin_function(body.clone(), param_bindings, receiver)?;
                        self.push_stack(ValueRef::new(ret_value))?;
                    }
                    _ => bail!(self, "Invalid callee: {}", target.value().deref()),
                }
            }
            OpCode::Index => {
                let index_value_ref = self.pop_stack()?;
                let target_ref = self.pop_stack()?;
                match (&*index_value_ref.value(), &*target_ref.value()) {
                    (Value::Int { value: index }, Value::List { value: list }) => {
                        if *index < 0 || *index >= list.len() as i32 {
                            bail!(
                                self,
                                "Index {} out of bounds for length {}",
                                *index,
                                list.len()
                            );
                        }
                        self.push_stack(ValueRef::new(list[*index as usize].clone()))?;
                    }
                    (_, _) => bail!(
                        self,
                        "Only List values can be indexed: {}",
                        target_ref.value().deref()
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
                    .pop_stack()?
                    .value()
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
                    .pop_stack()?
                    .value()
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
                let entry = self.constant_pool
                    [operand_2bytes(self.get_code()?, operand_candidate)? as usize]
                    .clone();
                match entry {
                    ConstantPoolEntry::MemberRef { member_name } => {
                        let receiver_id = self.receiver_table.len();
                        self.receiver_table.push(receiver_ref.clone());

                        match receiver_ref.value().find_member(&member_name, receiver_id) {
                            None => bail!(self, "Non-existent member: {}", member_name),
                            Some(v) => {
                                self.push_stack(v)?;
                            }
                        }
                    }
                    ConstantPoolEntry::Value { .. } => {
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
                    let item = self.pop_stack()?;
                    items.push(item.value().clone());
                }
                items.reverse();
                self.push_stack(ValueRef::new(Value::List { value: items }))?;
            }
            OpCode::Not => {
                let target = self.pop_stack();
                self.push_stack(ValueRef::new(Value::not(&target?.value())?))?;
            }
            OpCode::Plus => {
                let target = self.pop_stack();
                self.push_stack(ValueRef::new(Value::plus(&target?.value())?))?;
            }
            OpCode::Minus => {
                let target = self.pop_stack();
                self.push_stack(ValueRef::new(Value::minus(&target?.value())?))?;
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

                self.push_stack(ValueRef::new(Value::Void))?;
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
        Ok(self.get_current_frame().borrow_mut().push_operand_stack(value))
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

    pub fn borrow_current_frame_mut(&self) -> RefMut<'_, StackFrame> {
        self.context.stack_frame_cache.borrow_frame_ref_mut()
    }

    fn update_frame_cache(&mut self) -> Result<(), RuntimeError> {
        Ok(self.context.stack_frame_cache.update_frame(
            self.context.call_stack.last().ok_or_else(|| error_without_pc!("Could not update stack frame."))?.clone()
        ))
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
fn setup_builtin_functions(identifier_slot: &mut HashMap<u16, ValueRef>) {
    identifier_slot.insert(
        0,
        ValueRef::new(
            Value::BuiltinFunction {
                name: "print".to_string(),
                param_count: 1,
                receiver_id: None,
                body: BuiltinFunctionKind::Print,
            }
        ),
    );
    identifier_slot.insert(
        1,
        ValueRef::new(
            Value::BuiltinFunction {
                name: "input".to_string(),
                param_count: 0,
                receiver_id: None,
                body: BuiltinFunctionKind::Input,
            }
        ),
    );
    identifier_slot.insert(
        2,
        ValueRef::new(
            Value::BuiltinFunction {
                name: "range".to_string(),
                param_count: 2,
                receiver_id: None,
                body: BuiltinFunctionKind::Range,
            }
        ),
    );
    identifier_slot.insert(
        3,
        ValueRef::new(
            Value::BuiltinFunction {
                name: "now".to_string(),
                param_count: 0,
                receiver_id: None,
                body: BuiltinFunctionKind::Now,
            }
        ),
    );
}

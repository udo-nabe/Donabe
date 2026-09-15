use crate::value::{Value, ValueRef};
use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;
use crate::error_with_pc;
use crate::vm::RuntimeError;

#[derive(PartialEq)]
struct Registers {
    pub pc: u32,
    pub sp: u32,
    pub stack_base: u32,
}

#[derive(PartialEq)]
pub struct StackFrame {
    name: String,
    parent: Option<Rc<RefCell<StackFrame>>>,
    registers: Registers,
    code: Rc<Vec<u8>>,
    locals: HashMap<u16, ValueRef>,
}

impl Registers {
    pub fn new(stack_base: u32) -> Registers {
        Registers {
            pc: 0,
            sp: 0,
            stack_base,
        }
    }
}

impl StackFrame {
    pub fn new(
        name: String,
        parent: Option<Rc<RefCell<StackFrame>>>,
        code: Rc<Vec<u8>>,
        stack_base: u32,
        locals: HashMap<u16, Value>,
    ) -> StackFrame {
        StackFrame {
            name,
            parent,
            registers: Registers::new(stack_base),
            code,
            locals: locals
                .into_iter()
                .map(|(k, v)| (k, ValueRef::new(v)))
                .collect(),
        }
    }

    pub fn increase_pc(&mut self, increasement: u32) {
        self.registers.pc += increasement;
    }

    pub fn set_pc(&mut self, pc: u32) {
        self.registers.pc = pc;
    }

    pub fn set_sp(&mut self, sp: u32) {
        self.registers.sp = sp;
    }
    
    pub fn increment_sp(&mut self) {
        self.registers.sp += 1;
    }
    
    pub fn decrement_sp(&mut self) {
        self.registers.sp -= 1;
    }

    pub fn pc(&self) -> u32 {
        self.registers.pc
    }

    pub fn sp(&self) -> u32 {
        self.registers.sp
    }

    pub fn stack_base(&self) -> u32 {
        self.registers.stack_base
    }

    pub fn get_local_var(&self, slot: u16) -> Option<ValueRef> {
        Some(self.locals.get(&slot)?.clone())
    }

    pub fn get_captured_var(&self, slot: u16) -> Option<ValueRef> {
        self.parent.as_deref()?.borrow().find_var_recursive(slot)
    }

    pub fn set_local_var(&mut self, slot: u16, value: ValueRef) -> Result<(), RuntimeError> {
        match self.locals.insert(slot, value) {
            None => Err(error_with_pc!(self.pc(), "Non-existent slot: {}", slot)),
            Some(_) => Ok(())
        }
    }

    pub fn set_captured_var(&self, slot: u16, value_ref: ValueRef) -> Result<(), RuntimeError> {
        match self.parent {
            None => Err(error_with_pc!(self.pc(), "Non-existent slot: {}", slot)),
            Some(ref parent) => parent.borrow_mut().set_var_recursive(slot, value_ref),
        }
    }

    pub fn name(&self) -> &str {
        &self.name
    }

    pub fn parent(&self) -> &Option<Rc<RefCell<StackFrame>>> {
        &self.parent
    }

    pub fn code(&self) -> &Vec<u8> {
        &self.code
    }

    pub fn locals(&self) -> &HashMap<u16, ValueRef> {
        &self.locals
    }

    fn find_var_recursive(&self, slot: u16) -> Option<ValueRef> {
        match self.get_local_var(slot) {
            Some(v) => Some(v),
            None => {
                if self.parent.is_some() {
                    self.parent.as_deref()?.borrow().find_var_recursive(slot)
                } else {
                    None
                }
            }
        }
    }

    fn set_var_recursive(&mut self, slot: u16, value_ref: ValueRef) -> Result<(), RuntimeError> {
        if self.locals.contains_key(&slot) {
            self.locals.insert(slot, value_ref);
            Ok(())
        } else {
            if self.parent.is_some() {
                match self.parent.as_deref() {
                    None => Err(error_with_pc!(self.pc(), "Non-existent slot: {}", slot)),
                    Some(parent) => parent.borrow_mut().set_var_recursive(slot, value_ref),
                }
            } else {
                Err(error_with_pc!(self.pc(), "Non-existent slot: {}", slot))
            }
        }
    }
}

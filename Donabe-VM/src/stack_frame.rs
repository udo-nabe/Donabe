use crate::value::{Value, ValueRef};
use std::cell::{Ref, RefCell};
use std::collections::{HashMap, HashSet};
use std::rc::Rc;
use crate::error_with_pc;
use crate::vm::RuntimeError;

pub type FrameRef = Rc<RefCell<StackFrame>>;

#[derive(PartialEq, Debug)]
struct Registers {
    pub pc: u32,
    pub sp: u32,
    pub stack_base: u32,
}

#[derive(PartialEq, Debug)]
pub struct StackFrame {
    name: String,
    parent: Option<Rc<RefCell<StackFrame>>>,
    registers: Registers,
    operand_stack: Vec<ValueRef>,
    code: Rc<Vec<u8>>,
    identifiers: Vec<ValueRef>,
    locals: HashSet<u16>,
}

impl Registers {
    pub fn new(stack_base: u32) -> Registers {
        Registers {
            pc: 0,
            sp: stack_base,
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
        identifiers: &Vec<ValueRef>,
        locals: HashSet<u16>,
    ) -> StackFrame {
        StackFrame {
            name,
            parent,
            registers: Registers::new(stack_base),
            operand_stack: Vec::with_capacity(10),
            code,
            identifiers: identifiers.clone(),
            locals,
        }
    }

    pub fn push_operand_stack(&mut self, value_ref: ValueRef) {
        self.operand_stack.push(value_ref);
    }

    pub fn pop_operand_stack(&mut self) -> Result<ValueRef, RuntimeError> {
        self.operand_stack.pop().ok_or_else(|| error_with_pc!(self.registers.pc, "Operand stack is empty"))
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
        Some(self.identifiers.get(slot as usize)?.clone())
    }

    pub fn get_captured_var(&self, slot: u16) -> Option<ValueRef> {
        self.parent.as_deref()?.borrow().find_var_recursive(slot)
    }

    pub fn set_local_var(&mut self, slot: u16, value: ValueRef) -> Result<(), RuntimeError> {
        self.identifiers[slot as usize] = value;
        Ok(())
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

    pub fn code(&self) -> Rc<Vec<u8>> {
        self.code.clone()
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
        if self.locals.contains(&slot) {
            self.identifiers[slot as usize] = value_ref;
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

    pub fn operand_stack(&self) -> &Vec<ValueRef> {
        &self.operand_stack
    }
}

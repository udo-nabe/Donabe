use crate::error_with_pc;
use crate::heap::handle::Handle;
use crate::heap::heap::Heap;
use crate::value::{Value, ValueRef};
use crate::vm::RuntimeError;
use std::cell::{Ref, RefCell};
use std::collections::{HashMap, HashSet};
use std::rc::Rc;

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
        locals_count: u16,
        undefined_handle: Handle,
    ) -> StackFrame {
        StackFrame {
            name,
            parent,
            registers: Registers::new(stack_base),
            operand_stack: Vec::with_capacity(10),
            code,
            identifiers: vec![undefined_handle; locals_count as usize],
        }
    }

    pub fn with_locals(
        name: String,
        parent: Option<Rc<RefCell<StackFrame>>>,
        code: Rc<Vec<u8>>,
        stack_base: u32,
        locals: Vec<Handle>,
    ) -> StackFrame {
        StackFrame {
            name,
            parent,
            registers: Registers::new(stack_base),
            operand_stack: Vec::with_capacity(10),
            code,
            identifiers: locals,
        }
    }

    pub fn push_operand_stack(&mut self, value_ref: ValueRef) {
        self.operand_stack.push(value_ref);
    }

    pub fn pop_operand_stack(&mut self) -> Result<ValueRef, RuntimeError> {
        self.operand_stack
            .pop()
            .ok_or_else(|| error_with_pc!(self.registers.pc, "Operand stack is empty"))
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

    pub fn get_captured_var(&self, depth: u16, slot: u16) -> Option<ValueRef> {
        self.get_var_with_depth(depth, slot)
    }

    pub fn set_local_var(&mut self, slot: u16, value: ValueRef) -> Result<(), RuntimeError> {
        self.identifiers[slot as usize] = value;
        Ok(())
    }

    pub fn set_captured_var(
        &mut self,
        depth: u16,
        slot: u16,
        value_ref: ValueRef,
    ) -> Result<(), RuntimeError> {
        self.set_var_with_depth(depth, slot, value_ref)
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

    fn get_var_with_depth(&self, depth: u16, slot: u16) -> Option<ValueRef> {
        if depth == 0 {
            return None;
        }

        let mut target_parent = self.parent.clone()?;

        for _ in 0..depth - 1 {
            let parent = target_parent.borrow().parent.clone()?;
            target_parent = parent;
        }

        target_parent.borrow().get_local_var(slot)
    }

    fn set_var_with_depth(
        &mut self,
        depth: u16,
        slot: u16,
        value_ref: ValueRef,
    ) -> Result<(), RuntimeError> {
        if depth == 0 {
            return Err(error_with_pc!(self.registers.pc, "The depth of instruction load_captured must not be 0."));
        }

        let mut target_parent = self
            .parent
            .as_ref()
            .ok_or_else(|| error_with_pc!(self.registers.pc, "Parent not found."))?
            .clone();

        for _ in 0..depth - 1 {
            let parent = target_parent
                .borrow()
                .parent
                .clone()
                .ok_or_else(|| error_with_pc!(self.registers.pc, "Parent not found."))?;
            target_parent = parent;
        }

        target_parent.borrow_mut().set_local_var(slot, value_ref)
    }

    pub fn operand_stack(&self) -> &Vec<ValueRef> {
        &self.operand_stack
    }
}

use std::cell::{Ref, RefMut};
use crate::stack_frame::{FrameRef, StackFrame};

pub struct StackFrameCache {
    current_frame: FrameRef,
}

impl StackFrameCache {
    pub fn new(root_frame: FrameRef) -> StackFrameCache {
        StackFrameCache {
            current_frame: root_frame,
        }
    }

    pub fn current_frame(&self) -> FrameRef {
        self.current_frame.clone()
    }
    
    pub fn borrow_frame_ref(&self) -> Ref<'_, StackFrame> {
        self.current_frame.borrow()
    }

    pub fn borrow_frame_ref_mut(&self) -> RefMut<'_, StackFrame> {
        self.current_frame.borrow_mut()
    }

    pub fn update_frame(&mut self, frame: FrameRef) {
        self.current_frame = frame;
    }
}

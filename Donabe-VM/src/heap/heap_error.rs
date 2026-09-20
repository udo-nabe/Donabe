use std::error::Error;
use std::fmt::{Display, Formatter};
use crate::allocator::alloc_error::AllocationError;

#[derive(Debug)]
pub enum HeapError {
    AllocationFailed(AllocationError),
    InvalidHandle(usize),
    PointerIsNull,
}

impl Display for HeapError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

impl Error for HeapError {}
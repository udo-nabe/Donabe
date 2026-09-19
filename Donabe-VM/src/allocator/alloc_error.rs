use std::alloc::LayoutError;
use std::error::Error;
use std::fmt::{Display, Formatter};

#[derive(Debug)]
pub enum AllocationError {
    LayoutError(LayoutError),
    AllocError,
}

impl Display for AllocationError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

impl Error for AllocationError {}
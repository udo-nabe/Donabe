use std::fmt::{write, Debug, Formatter};

#[derive(Clone, Copy, PartialEq)]
pub struct Handle(pub(super) usize);

impl Debug for Handle {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "Handle(0x{:x})", self.0)
    }
}
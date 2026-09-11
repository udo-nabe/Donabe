use crate::bytecode::section::{CodeSection, ConstantPoolSection, IdentifiersSection};

pub mod section;

#[derive(Debug)]
pub struct ByteCode {
    pub constant_pool: ConstantPoolSection,
    pub identifiers: IdentifiersSection,
    pub code_section: CodeSection,
}
use crate::bytecode::section::{InitializationCodeSection, ConstantPoolSection, GlobalIdentifiersSection};

pub mod section;

#[derive(Debug)]
pub struct ByteCode {
    pub constant_pool: ConstantPoolSection,
    pub identifiers: GlobalIdentifiersSection,
    pub code_section: InitializationCodeSection,
}
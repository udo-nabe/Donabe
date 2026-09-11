use crate::value::Value;
use std::collections::HashSet;

pub const CODE_SECTION_TYPE: u8 = 0x01;
pub const CONSTANT_POOL_SECTION_TYPE: u8 = 0x02;
pub const IDENTIFIERS_SECTION_TYPE: u8 = 0x03;
pub const IDENTIFIER_SLOT_SIZE: u16 = 0x02;

pub const CONSTANT_POOL_MEMBER_REF_TYPE: u8 = 0x01;
pub const CONSTANT_POOL_VALUE_TYPE: u8 = 0x02;

#[derive(Debug)]
pub enum SectionCreateError {
    TooManyEntries,
    TooLongCode,
}

#[derive(Debug)]
pub enum ConstantPoolEntry {
    MemberRef { member_name: String },
    Value { value: Value },
}

#[derive(Debug)]
pub struct ConstantPoolSection {
    count: u16,
    values: Vec<ConstantPoolEntry>,
}

#[derive(Debug)]
pub struct IdentifiersSection {
    count: u16,
    slots: HashSet<u16>,
}

#[derive(Debug)]
pub struct CodeSection {
    size: u32,
    code: Vec<u8>,
}

impl ConstantPoolSection {
    pub fn new(values: Vec<ConstantPoolEntry>) -> Result<ConstantPoolSection, SectionCreateError> {
        if values.len() >= 0xffff {
            return Err(SectionCreateError::TooManyEntries);
        }
        Ok(ConstantPoolSection {
            count: values.len() as u16,
            values,
        })
    }
}

impl IdentifiersSection {
    pub fn new(slots: HashSet<u16>) -> Result<IdentifiersSection, SectionCreateError> {
        if slots.len() >= 0xffff {
            return Err(SectionCreateError::TooManyEntries);
        }
        Ok(IdentifiersSection {
            count: slots.len() as u16,
            slots,
        })
    }
}

impl CodeSection {
    pub fn new(code: Vec<u8>) -> Result<CodeSection, SectionCreateError> {
        if code.len() >= 0xff_ff_ff_ff {
            return Err(SectionCreateError::TooLongCode);
        }

        Ok(CodeSection {
            size: code.len() as u32,
            code,
        })
    }
}

use crate::value::Value;
use std::collections::HashSet;
use std::fmt::{write, Debug, Formatter};

pub const CODE_SECTION_TYPE: u8 = 0x01;
pub const CONSTANT_POOL_SECTION_TYPE: u8 = 0x02;
pub const IDENTIFIERS_SECTION_TYPE: u8 = 0x03;
pub const IDENTIFIER_SLOT_SIZE: u16 = 0x02;

pub const CONSTANT_POOL_MEMBER_REF_TYPE: u8 = 0x01;
pub const CONSTANT_POOL_VALUE_TYPE: u8 = 0x02;

#[derive(Debug, Clone)]
pub enum SectionCreateError {
    TooManyEntries,
    TooLongCode,
}

#[derive(Clone)]
pub enum ConstantPoolEntry {
    MemberRef { member_name: String },
    Value { value: Value },
}

#[derive(Debug, Clone)]
pub struct ConstantPoolSection {
    count: u16,
    values: Vec<ConstantPoolEntry>,
}

#[derive(Debug, Clone)]
pub struct IdentifiersSection {
    count: u16,
    slots: HashSet<u16>,
}

#[derive(Debug, Clone)]
pub struct CodeSection {
    size: u32,
    code: Vec<u8>,
}

impl Debug for ConstantPoolEntry {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            ConstantPoolEntry::MemberRef { member_name } => write!(f, "MemberRef: {member_name}"),
            ConstantPoolEntry::Value { value } => write!(f, "Value: {}", value)
        }
    }
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
    
    pub fn values(&self) -> Vec<ConstantPoolEntry> {
        self.values.clone()
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
    
    pub fn slots(&self) -> HashSet<u16> {
        self.slots.clone()
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
    
    pub fn code(&self) -> Vec<u8> {
        self.code.clone()
    }
}

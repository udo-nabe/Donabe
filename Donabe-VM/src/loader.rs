use crate::bytecode::ByteCode;
use crate::bytecode::section::{CODE_SECTION_TYPE, CONSTANT_POOL_MEMBER_REF_TYPE, CONSTANT_POOL_SECTION_TYPE, InitializationCodeSection, ConstantPoolEntry, ConstantPoolSection, IDENTIFIER_SLOT_SIZE, IDENTIFIERS_SECTION_TYPE, GlobalIdentifiersSection, SectionCreateError, CONSTANT_POOL_VALUE_TYPE, CONSTANT_POOL_GLOBAL_REF_TYPE};
use crate::value::Value;
use byteorder::ReadBytesExt;
use std::collections::{HashMap, HashSet};
use std::fs::{File, read};
use std::io;
use std::io::{BufReader, Read, Seek};
use std::rc::Rc;
use std::string::FromUtf8Error;

#[derive(Debug)]
pub enum LoadError {
    Io(io::Error),
    SizeMismatch(String),
    IdentifierSlotsOverlap,
    LocalSlotOverlap,
    SectionOverlap,
    MissingConstantPoolSection,
    MissingIdentifiersSection,
    MissingCodeSection,
    FailedToCreateSection(SectionCreateError),
    InvalidSectionType,
    UnknownConstantPoolEntryType(u8),
    UnknownValueType(u8),
    InvalidUtf8(FromUtf8Error),
    InvalidSize,
    InvalidBool,
}

pub fn load_file(reader: &mut BufReader<File>) -> Result<ByteCode, LoadError> {
    let section_size = read_4bytes(reader)?;
    let mut identifiers_section: GlobalIdentifiersSection = load_identifiers_section(reader, section_size)?;

    let section_size = read_4bytes(reader)?;
    let mut constant_pool_section: ConstantPoolSection = load_constant_pool_section(reader, section_size)?;

    let section_size = read_4bytes(reader)?;
    let mut code_section: InitializationCodeSection = load_code_section(reader, section_size)?;

    Ok(ByteCode {
        constant_pool: constant_pool_section,
        identifiers: identifiers_section,
        code_section,
    })
}

fn load_code_section(reader: &mut dyn Read, size: u32) -> Result<InitializationCodeSection, LoadError> {
    Ok(InitializationCodeSection::new(read_any_bytes(reader, size as usize)?)
        .map_err(LoadError::FailedToCreateSection)?)
}

fn load_identifiers_section(
    reader: &mut dyn Read,
    size: u32,
) -> Result<GlobalIdentifiersSection, LoadError> {
    let identifiers_count = read_2bytes(reader)?;

    let mut result = HashSet::with_capacity(identifiers_count as usize);
    let mut sum_size = 0x02;

    for _ in 0..identifiers_count {
        let size = read_4bytes(reader)?;
        result.insert(read_utf8(reader, size)?);

        sum_size += 0x04 + size;
    }

    if size != sum_size {
        return Err(LoadError::SizeMismatch(format!("Identifier slots. expected: {:x} actual: {:x}", size, sum_size)));
    }

    Ok(GlobalIdentifiersSection::new(result).map_err(LoadError::FailedToCreateSection)?)
}

fn load_constant_pool_section(
    reader: &mut dyn Read,
    size: u32,
) -> Result<ConstantPoolSection, LoadError> {
    let pool_count = read_2bytes(reader)?;
    let mut sum_size = 0x02;

    let mut result = Vec::new();

    for _ in 0..pool_count {
        let entry_type = reader.read_u8().map_err(LoadError::Io)?;
        let entry_size = read_4bytes(reader)?;

        let entry = match entry_type {
            CONSTANT_POOL_MEMBER_REF_TYPE => load_member_ref_entry(reader, entry_size),
            CONSTANT_POOL_VALUE_TYPE => load_value_entry(reader, entry_size),
            CONSTANT_POOL_GLOBAL_REF_TYPE => load_global_ref_entry(reader, entry_size),
            _ => Err(LoadError::UnknownConstantPoolEntryType(entry_type)),
        }?;

        result.push(entry);
        sum_size += 0x01 + 0x04 + entry_size;
    }

    if sum_size != size {
        return Err(LoadError::SizeMismatch(format!("Constant pool entry expected: {size} actual: {:}", sum_size)));
    }

    Ok(ConstantPoolSection::new(result).map_err(LoadError::FailedToCreateSection)?)
}

fn load_member_ref_entry(
    reader: &mut dyn Read,
    entry_size: u32,
) -> Result<ConstantPoolEntry, LoadError> {
    Ok(ConstantPoolEntry::MemberRef {
        member_name: read_utf8(reader, entry_size)?,
    })
}

fn load_value_entry(
    reader: &mut dyn Read,
    entry_size: u32,
) -> Result<ConstantPoolEntry, LoadError> {
    let value_type = reader.read_u8().map_err(LoadError::Io)?;
    let value_size = read_4bytes(reader)?;

    if entry_size != 1 /* value_type */ + 4 /* value_size */ + value_size {
        return Err(LoadError::SizeMismatch("Constant pool value entry".to_string()));
    }

    Ok(ConstantPoolEntry::Value {
        value: load_value(reader, value_type, value_size)?,
    })
}

fn load_global_ref_entry(
    reader: &mut dyn Read,
    entry_size: u32,
) -> Result<ConstantPoolEntry, LoadError> {
    Ok(ConstantPoolEntry::GlobalRef {
        name: read_utf8(reader, entry_size)?,
    })
}

fn load_value(reader: &mut dyn Read, value_type: u8, value_size: u32) -> Result<Value, LoadError> {
    match value_type {
        Value::BOOL_TYPE => load_bool_value_entry(reader, value_size),
        Value::INT_TYPE => load_int_value_entry(reader, value_size),
        Value::INT64_TYPE => load_int64_value_entry(reader, value_size),
        Value::STRING_TYPE => load_string_value_entry(reader, value_size),
        Value::FUNCTION_TYPE => load_function_value_entry(reader, value_size),
        Value::LIST_TYPE => load_list_entry(reader, value_size),
        _ => Err(LoadError::UnknownValueType(value_type)),
    }
}

fn load_bool_value_entry(reader: &mut dyn Read, entry_size: u32) -> Result<Value, LoadError> {
    if entry_size != 0x01 {
        return Err(LoadError::InvalidSize);
    }

    let value = reader.read_u8().map_err(LoadError::Io)?;

    if value == 0x00 {
        Ok(Value::Bool { value: false })
    } else if value == 0x01 {
        Ok(Value::Bool { value: true })
    } else {
        Err(LoadError::InvalidBool)
    }
}

fn load_int_value_entry(reader: &mut dyn Read, entry_size: u32) -> Result<Value, LoadError> {
    if entry_size != 0x04 {
        return Err(LoadError::InvalidSize);
    }

    Ok(Value::Int {
        value: read_4bytes(reader)? as i32,
    })
}

fn load_int64_value_entry(reader: &mut dyn Read, entry_size: u32) -> Result<Value, LoadError> {
    if entry_size != 0x08 {
        return Err(LoadError::InvalidSize);
    }

    Ok(Value::Int64 {
        value: read_8bytes(reader)? as i64,
    })
}

fn load_string_value_entry(reader: &mut dyn Read, entry_size: u32) -> Result<Value, LoadError> {
    Ok(Value::String {
        value: read_utf8(reader, entry_size)?,
    })
}

fn load_function_value_entry(reader: &mut dyn Read, entry_size: u32) -> Result<Value, LoadError> {
    let name_size = read_4bytes(reader)?;
    let name = read_utf8(reader, name_size)?;

    let param_count = read_2bytes(reader)?;
    let mut params = Vec::new();

    for _ in 0..param_count {
        params.push(read_2bytes(reader)?);
    }

    let local_count = read_2bytes(reader)?;

    let code_size = read_4bytes(reader)?;
    let code = read_any_bytes(reader, code_size as usize)?;
    let actual_size = 0x04 + name_size
        + 0x02 + param_count as u32 * 0x02
        + 0x02
        + 0x04
        + code_size;

    if entry_size != actual_size {
        return Err(LoadError::SizeMismatch("Load function".to_string()));
    }

    Ok(Value::Function {
        name,
        params,
        local_count,
        code: Rc::new(code),
    })
}

fn load_list_entry(reader: &mut dyn Read, entry_size: u32) -> Result<Value, LoadError> {
    let count = read_4bytes(reader)?;
    let mut value = Vec::new();

    let mut sum_size = 0x04;
    for _ in 0..count {
        let item_type = reader.read_u8().map_err(LoadError::Io)?;
        let item_size = read_4bytes(reader)?;
        let item = load_value(reader, item_type, item_size)?;

        value.push(item);
        sum_size += 0x01 + 0x04 + item_size;
    }

    if sum_size != entry_size {
        return Err(LoadError::SizeMismatch("List size".to_string()));
    }

    Ok(Value::List { value })
}

fn read_utf8(reader: &mut dyn Read, size: u32) -> Result<String, LoadError> {
    let utf8 = String::from_utf8(read_any_bytes(reader, size as usize)?)
        .map_err(LoadError::InvalidUtf8)?;
    Ok(utf8)
}

fn read_any_bytes(reader: &mut dyn Read, size: usize) -> Result<Vec<u8>, LoadError> {
    let mut buf = vec![0; size];
    reader.read_exact(&mut buf).map_err(LoadError::Io)?;
    Ok(buf)
}

fn read_8bytes(reader: &mut dyn Read) -> Result<u64, LoadError> {
    let mut buf = [0; 8];
    reader.read_exact(&mut buf).map_err(LoadError::Io)?;
    Ok(u64::from_le_bytes(buf))
}

fn read_4bytes(reader: &mut dyn Read) -> Result<u32, LoadError> {
    let mut buf = [0; 4];
    reader.read_exact(&mut buf).map_err(LoadError::Io)?;
    Ok(u32::from_le_bytes(buf))
}

fn read_2bytes(reader: &mut dyn Read) -> Result<u16, LoadError> {
    let mut buf = [0; 2];
    reader.read_exact(&mut buf).map_err(LoadError::Io)?;
    Ok(u16::from_le_bytes(buf))
}

use std::io;
use std::io::Read;

const EXPECTED_MAGIC: [u8; 4] = *b"\x00DNB";
const SUPPORTED_FILE_VERSION: [u8; 1] = [0x01];
const SUPPORTED_LANG_VERSION: [u8; 1] = [0x01];

#[derive(Debug)]
pub enum HeaderError {
    Io(io::Error),
    InvalidMagic,
    UnsupportedFileVersion(u8),
    UnsupportedLangVersion(u8),
}

pub fn check_header(reader: &mut dyn Read) -> Result<(), HeaderError> {
    let magic = read_bytes(reader, 4)?;
    if magic != EXPECTED_MAGIC {
        return Err(HeaderError::InvalidMagic);
    }

    let file_version = read_bytes(reader, 1)?;
    if file_version != SUPPORTED_FILE_VERSION {
        return Err(HeaderError::UnsupportedFileVersion(file_version[0]));
    }

    let lang_version = read_bytes(reader, 1)?;
    if lang_version != SUPPORTED_LANG_VERSION {
        return Err(HeaderError::UnsupportedLangVersion(lang_version[0]));
    }

    Ok(())
}

fn read_bytes(reader: &mut dyn Read, bytes: usize) -> Result<Vec<u8>, HeaderError> {
    let mut data = vec![0; bytes];
    reader.read_exact(&mut data).map_err(HeaderError::Io)?;
    Ok(data)
}

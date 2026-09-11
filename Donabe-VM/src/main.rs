mod header;
mod instruction;
mod value;
mod loader;
mod bytecode;

use crate::header::{HeaderError, check_header};
use clap::Parser;
use std::fmt::Debug;
use std::fs::File;
use std::io::{BufReader, Read};
use std::path::PathBuf;
use crate::bytecode::ByteCode;
use crate::loader::{load_file, LoadError};

#[derive(Parser, Debug)]
#[command(name = "Donabe VM")]
#[command(about = "Donabe default VM")]
struct CliArgs {
    file: PathBuf,
}

fn main() {
    let args = CliArgs::parse();

    let mut reader = BufReader::new(File::open(&args.file).expect("Unable to open .dnbc file."));

    if let Err(err) = check_header(&mut reader) {
        eprintln!(
            "{:?}",
            match err {
                HeaderError::Io(io_err) => format!("Unable to read header: {:?}", io_err),
                HeaderError::InvalidMagic => "Invalid Magic.".to_string(),
                HeaderError::UnsupportedFileVersion(ver) =>
                    format!("Unsupported file version: {}", ver),
                HeaderError::UnsupportedLangVersion(ver) =>
                    format!("Unsupported language version: {}", ver),
            }
        );
        std::process::exit(1);
    }

    let bytecode = match load_file(&mut reader) {
        Ok(v) => v,
        Err(e) => {
            eprintln!("Load failed. {:?}", e);
            std::process::exit(1);
        }
    };

    println!("{:?}", bytecode);
}

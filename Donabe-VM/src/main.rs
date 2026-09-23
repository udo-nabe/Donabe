mod bytecode;
mod header;
mod instruction;
mod loader;
mod stack_frame;
mod value;
mod vm;
mod builtin_functions;
mod stack_frame_cache;
mod allocator;
mod heap;

use crate::bytecode::ByteCode;
use crate::header::{HeaderError, check_header};
use crate::loader::{LoadError, load_file};
use crate::vm::VM;
use clap::Parser;
use std::fmt::Debug;
use std::fs::File;
use std::io::{BufReader};
use std::path::PathBuf;
use crate::value::Value;

#[derive(Parser, Debug)]
#[command(name = "Donabe VM")]
#[command(about = "Donabe default VM")]
struct CliArgs {
    file: PathBuf,
}

fn main() {
    println!("Value size: {}", size_of::<Value>());

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

    let mut vm = match VM::new(&bytecode) {
        Ok(v) => v,
        Err(e) => {
            eprintln!("Could not make VM. {:?}", e);
            std::process::exit(1);
        }
    };
    if let Err(err) = vm.run() {
        eprintln!("Error: {}", err);
    }
}

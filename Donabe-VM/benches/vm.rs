use std::fs::File;
use std::io::BufReader;
use criterion::{criterion_group, Criterion, criterion_main};
use DonabeVM::header::{check_header, HeaderError};
use DonabeVM::loader::load_file;
use DonabeVM::vm::VM;

fn benchmark_vm(c: &mut Criterion) {
    let mut reader = BufReader::new(File::open("../test.dnbc").expect("Unable to open .dnbc file."));

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

    c.bench_function("vm", |b| {
        b.iter(|| {
            let mut vm = VM::new(&bytecode).unwrap();
            if let Err(err) = vm.run() {
                eprintln!("Error: {}", err);
            }
        });
    });
}

criterion_group!(benches, benchmark_vm);
criterion_main!(benches);
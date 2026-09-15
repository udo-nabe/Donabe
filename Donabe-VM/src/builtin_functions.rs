use crate::value::{BuiltinFunctionKind, Value, ValueRef};
use crate::vm::RuntimeError;
use std::io;
use crate::error_without_pc;

fn builtin_print(params: Vec<ValueRef>) -> Result<Value, RuntimeError> {
    println!("{}", params[0].value());
    Ok(Value::Void)
}

fn builtin_input(_: Vec<ValueRef>) -> Result<Value, RuntimeError> {
    let mut buf = String::new();
    io::stdin()
        .read_line(&mut buf)
        .map_err(|e| error_without_pc!("Failed to input from stdin: {}", e))?;
    Ok(Value::String {
        value: buf.trim().to_string(),
    })
}

fn builtin_range(params: Vec<ValueRef>) -> Result<Value, RuntimeError> {
    let start_inclusive = params[0].value().expect_int()?;
    let end_exclusive = params[1].value().expect_int()?;

    Ok(Value::List {
        value: (start_inclusive..end_exclusive)
            .map(|i| Value::Int { value: i })
            .collect(),
    })
}

fn builtin_to_string(params: Vec<ValueRef>, receiver: ValueRef) -> Result<Value, RuntimeError> {
    Ok(Value::String {
        value: receiver.value().to_string()
    })
}

fn builtin_list_length(params: Vec<ValueRef>, receiver: ValueRef) -> Result<Value, RuntimeError> {
    let value = receiver.value();
    Ok(Value::Int {
        value: value.expect_list()?.len() as i32
    })
}

fn builtin_string_length(params: Vec<ValueRef>, receiver: ValueRef) -> Result<Value, RuntimeError> {
    let value = receiver.value();
    Ok(Value::Int {
        value: value.expect_string()?.len() as i32
    })
}

/// paramsは、適切な長さでなければならない。
pub fn dispatch_builtin_function(kind: BuiltinFunctionKind, params: Vec<ValueRef>, receiver: Option<ValueRef>) -> Result<Value, RuntimeError> {
    match kind {
        BuiltinFunctionKind::Print => builtin_print(params),
        BuiltinFunctionKind::Input => builtin_input(params),
        BuiltinFunctionKind::Range => builtin_range(params),
        BuiltinFunctionKind::ToString => builtin_to_string(params, receiver.expect("Any#toString() requires receiver.")),
        BuiltinFunctionKind::ListLength => builtin_list_length(params, receiver.expect("List#length() requires receiver.")),
        BuiltinFunctionKind::StringLength => builtin_string_length(params, receiver.expect("String#length() requires receiver.")),
    }
}
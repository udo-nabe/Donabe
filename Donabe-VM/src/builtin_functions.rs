use crate::value::{BuiltinFunctionKind, Value, ValueRef};
use crate::vm::RuntimeError;
use std::io;
use std::time::{SystemTime, UNIX_EPOCH};
use crate::error_without_pc;
use crate::heap::heap::Heap;

fn builtin_print(params: Vec<ValueRef>, heap: &Heap) -> Result<Value, RuntimeError> {
    println!("{}", heap.get(params[0]).map_err(|_| error_without_pc!("Could not take arg."))?);
    Ok(Value::Void)
}

fn builtin_input() -> Result<Value, RuntimeError> {
    let mut buf = String::new();
    io::stdin()
        .read_line(&mut buf)
        .map_err(|e| error_without_pc!("Failed to input from stdin: {}", e))?;
    Ok(Value::String {
        value: buf.trim().to_string(),
    })
}

fn builtin_range(params: Vec<ValueRef>, heap: &Heap) -> Result<Value, RuntimeError> {
    let start_inclusive = heap.get(params[0]).map_err(|_| error_without_pc!("Could not take arg."))?.expect_int()?;
    let end_exclusive = heap.get(params[1]).map_err(|_| error_without_pc!("Could not take arg."))?.expect_int()?;

    Ok(Value::List {
        value: (start_inclusive..end_exclusive)
            .map(|i| Value::Int { value: i })
            .collect(),
    })
}

fn builtin_now() -> Value {
    let millis = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_millis() as i64;
    Value::Int64 { value: millis }
}

fn builtin_to_string(receiver: ValueRef, heap: &Heap) -> Result<Value, RuntimeError> {
    Ok(Value::String {
        value: heap.get(receiver).map_err(|_| error_without_pc!("Could not take arg."))?.to_string()
    })
}

fn builtin_list_length(receiver: ValueRef, heap: &Heap) -> Result<Value, RuntimeError> {
    let value = heap.get(receiver).map_err(|_| error_without_pc!("Could not take arg."))?;
    Ok(Value::Int {
        value: value.expect_list()?.len() as i32
    })
}

fn builtin_string_length(receiver: ValueRef, heap: &Heap) -> Result<Value, RuntimeError> {
    let value = heap.get(receiver).map_err(|_| error_without_pc!("Could not take arg."))?;
    Ok(Value::Int {
        value: value.expect_string()?.len() as i32
    })
}

/// paramsは、適切な長さでなければならない。
pub fn dispatch_builtin_function(kind: BuiltinFunctionKind, params: Vec<ValueRef>, receiver: Option<ValueRef>, heap: &Heap) -> Result<Value, RuntimeError> {
    match kind {
        BuiltinFunctionKind::Print => builtin_print(params, heap),
        BuiltinFunctionKind::Input => builtin_input(),
        BuiltinFunctionKind::Range => builtin_range(params, heap),
        BuiltinFunctionKind::Now => Ok(builtin_now()),
        BuiltinFunctionKind::ToString => builtin_to_string(receiver.expect("Any#toString() requires receiver."), heap),
        BuiltinFunctionKind::ListLength => builtin_list_length(receiver.expect("List#length() requires receiver."), heap),
        BuiltinFunctionKind::StringLength => builtin_string_length(receiver.expect("String#length() requires receiver."), heap),
    }
}
use crate::error_without_pc;
use crate::heap::handle::Handle;
use crate::heap::heap::Heap;
use crate::heap::heap_error::HeapError;
use crate::stack_frame::FrameRef;
use crate::vm::RuntimeError;
use std::collections::{HashMap, HashSet};
use std::fmt::{Debug, Display, Formatter};
use std::rc::Rc;

pub type ValueRef = Handle;

#[derive(Clone, PartialEq)]
pub enum BuiltinFunctionKind {
    // 通常の関数
    Print,
    Input,
    Range,
    Now,

    // メソッド
    ToString,
    ListLength,
    StringLength,
}

#[derive(Clone, PartialEq)]
pub enum Value {
    Bool {
        value: bool,
    },
    Int {
        value: i32,
    },
    Int64 {
        value: i64,
    },
    String {
        value: String,
    },
    Function {
        name: String,
        params: Vec<u16>,
        local_count: u16,
        code: Rc<Vec<u8>>,
    },
    Closure {
        function_handle: Handle,
        parent: FrameRef,
    },
    BuiltinFunction {
        name: String,
        param_count: u32,
        receiver_id: Option<usize>,
        body: BuiltinFunctionKind,
    },
    List {
        value: Vec<Value>,
    },
    Undefined,
    Void,
}

impl Debug for Value {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Value::String { value } => write!(f, "\"{}\"", value),
            other => self::Debug::fmt(other, f),
        }
    }
}

impl Display for Value {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let str = match self {
            Value::String { value } => format!("{}", value),
            Value::Function { params, .. } => format!("({} args) -> ?", params.len()),
            Value::Closure { function_handle, .. } => "<closure>".to_string(),
            Value::BuiltinFunction { param_count, .. } => format!("({} args) -> ?", param_count),
            Value::List { value } => {
                format!(
                    "[{}]",
                    value
                        .iter()
                        .map(|v| format!("{}", v))
                        .collect::<Vec<_>>()
                        .join(", ")
                )
            }
            Value::Undefined => "<undefined>".to_string(),
            Value::Void => "<void>".to_string(),
            Value::Bool { value } => format!("{}", value),
            Value::Int { value } => format!("{}", value),
            Value::Int64 { value } => format!("{}", value),
        };
        write!(f, "{}", str)
    }
}

impl Value {
    pub fn expect_int(&self) -> Result<i32, RuntimeError> {
        match self {
            Value::Int { value } => Ok(*value),
            _ => Err(error_without_pc!("Failed to convert {} to i32", self)),
        }
    }

    pub fn expect_bool(&self) -> Result<bool, RuntimeError> {
        match self {
            Value::Bool { value } => Ok(*value),
            _ => Err(error_without_pc!("Failed to convert {} to bool", self)),
        }
    }

    pub fn expect_list(&self) -> Result<&Vec<Value>, RuntimeError> {
        match self {
            Value::List { value } => Ok(value),
            _ => Err(error_without_pc!("Failed to convert {} to bool", self)),
            _ => Err(error_without_pc!("Failed to convert {} to bool", self)),
        }
    }

    pub fn expect_string(&self) -> Result<&str, RuntimeError> {
        match self {
            Value::String { value } => Ok(value),
            _ => Err(error_without_pc!("Failed to convert {} to bool", self)),
        }
    }

    pub fn members(&self, receiver_id: usize) -> Result<HashMap<String, Value>, HeapError> {
        let mut result = HashMap::new();

        result.insert(
            "toString".to_string(),
            Value::BuiltinFunction {
                name: "toString".to_string(),
                param_count: 0,
                receiver_id: Some(receiver_id),
                body: BuiltinFunctionKind::ToString,
            },
        );
        Ok(result)
    }

    pub fn find_member(&self, member_name: &String, receiver_id: usize) -> Option<Value> {
        self.members(receiver_id).ok()?.get(member_name).cloned()
    }

    pub fn add(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        match (lhs, rhs) {
            (Value::Int { value: l }, Value::Int { value: r }) => Ok(Value::Int {
                value: l.wrapping_add(*r),
            }),
            (Value::String { value: l }, Value::String { value: r }) => Ok(Value::String {
                value: format!("{l}{r}"),
            }),
            (_, _) => Err(error_without_pc!(
                "Operator '+' cannot be applied to types {} and {}",
                lhs,
                rhs
            )),
        }
    }

    pub fn sub(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        match (lhs, rhs) {
            (Value::Int { value: l }, Value::Int { value: r }) => Ok(Value::Int {
                value: l.wrapping_sub(*r),
            }),
            (Value::Int64 { value: l }, Value::Int64 { value: r }) => Ok(Value::Int64 {
                value: l.wrapping_sub(*r),
            }),
            (_, _) => Err(error_without_pc!(
                "Operator '-' cannot be applied to types {} and {}",
                lhs,
                rhs
            )),
        }
    }

    pub fn mul(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        match (lhs, rhs) {
            (Value::Int { value: l }, Value::Int { value: r }) => Ok(Value::Int {
                value: l.wrapping_mul(*r),
            }),
            (_, _) => Err(error_without_pc!(
                "Operator '*' cannot be applied to types {} and {}",
                lhs,
                rhs
            )),
        }
    }

    pub fn div(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        match (lhs, rhs) {
            (Value::Int { value: l }, Value::Int { value: r }) => Ok(Value::Int {
                value: l.wrapping_div(*r),
            }),
            (_, _) => Err(error_without_pc!(
                "Operator '/' cannot be applied to types {} and {}",
                lhs,
                rhs
            )),
        }
    }

    pub fn equal(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        Ok(Value::Bool { value: lhs == rhs })
    }

    pub fn greater(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        match (lhs, rhs) {
            (Value::Int { value: l }, Value::Int { value: r }) => Ok(Value::Bool { value: l > r }),
            (_, _) => Err(error_without_pc!(
                "Operator '>' cannot be applied to types {} and {}",
                lhs,
                rhs
            )),
        }
    }

    pub fn greater_equal(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        match (lhs, rhs) {
            (Value::Int { value: l }, Value::Int { value: r }) => Ok(Value::Bool { value: l >= r }),
            (_, _) => Err(error_without_pc!(
                "Operator '>=' cannot be applied to types {} and {}",
                lhs,
                rhs
            )),
        }
    }

    pub fn less(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        match (lhs, rhs) {
            (Value::Int { value: l }, Value::Int { value: r }) => Ok(Value::Bool { value: l < r }),
            (_, _) => Err(error_without_pc!(
                "Operator '<' cannot be applied to types {} and {}",
                lhs,
                rhs
            )),
        }
    }

    pub fn less_equal(lhs: &Value, rhs: &Value) -> Result<Value, RuntimeError> {
        match (lhs, rhs) {
            (Value::Int { value: l }, Value::Int { value: r }) => Ok(Value::Bool { value: l <= r }),
            (_, _) => Err(error_without_pc!(
                "Operator '<=' cannot be applied to types {} and {}",
                lhs,
                rhs
            )),
        }
    }

    pub fn not(target: &Value) -> Result<Value, RuntimeError> {
        match target {
            Value::Bool { value } => Ok(Value::Bool { value: !*value }),
            _ => Err(error_without_pc!(
                "Operator '!' cannot be applied to type {}",
                target
            )),
        }
    }

    pub fn plus(target: &Value) -> Result<Value, RuntimeError> {
        match target {
            Value::Int { value } => Ok(Value::Int { value: *value }),
            _ => Err(error_without_pc!(
                "Operator '+' cannot be applied to type {}",
                target
            )),
        }
    }

    pub fn minus(target: &Value) -> Result<Value, RuntimeError> {
        match target {
            Value::Int { value } => Ok(Value::Int { value: -*value }),
            _ => Err(error_without_pc!(
                "Operator '-' cannot be applied to type {}",
                target
            )),
        }
    }
}

impl Value {
    pub const BOOL_TYPE: u8 = 0x01;
    pub const INT_TYPE: u8 = 0x02;
    pub const INT64_TYPE: u8 = 0x03;
    pub const STRING_TYPE: u8 = 0x04;
    pub const FUNCTION_TYPE: u8 = 0x05;
    pub const LIST_TYPE: u8 = 0x06;
}

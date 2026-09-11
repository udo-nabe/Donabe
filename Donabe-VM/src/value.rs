use std::collections::HashSet;

#[derive(Debug)]
pub enum Value {
    Bool {
        value: bool,
    },
    Int {
        value: i32,
    },
    String {
        value: String,
    },
    Function {
        name: String,
        params: Vec<u16>,
        locals: HashSet<u16>,
        code: Vec<u8>,
    },
    List {
        value: Vec<Value>,
    },
}

impl Value {
    pub const BOOL_TYPE: u8 = 0x01;
    pub const INT_TYPE: u8 = 0x02;
    pub const STRING_TYPE: u8 = 0x03;
    pub const FUNCTION_TYPE: u8 = 0x04;
    pub const LIST_TYPE: u8 = 0x05;
}

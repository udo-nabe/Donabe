use crate::allocator::bump_allocator::{BumpAllocator, DEFAULT_CHUNK_SIZE};
use crate::heap::handle::Handle;
use crate::heap::heap_error::HeapError;
use crate::value::Value;
use std::alloc::Layout;
use std::num::NonZero;
use std::ptr::NonNull;

pub struct Heap {
    allocator: BumpAllocator,
    allocate_count: usize,

    bool_count: usize,
    int_count: usize,
    int64_count: usize,
    string_count: usize,
    function_count: usize,
    closure_count: usize,
    builtin_function_count: usize,
    list_count: usize,
    undefined_count: usize,
    void_count: usize,
}

impl Heap {
    pub fn new() -> Result<Heap, HeapError> {
        Ok(Heap {
            allocator: BumpAllocator::new(DEFAULT_CHUNK_SIZE, align_of::<Value>())
                .map_err(HeapError::AllocationFailed)?,
            allocate_count: 0,

            bool_count: 0,
            int_count: 0,
            int64_count: 0,
            string_count: 0,
            function_count: 0,
            closure_count: 0,
            builtin_function_count: 0,
            list_count: 0,
            undefined_count: 0,
            void_count: 0,
        })
    }

    #[inline(never)]
    pub fn alloc(&mut self, value: Value) -> Result<Handle, HeapError> {
        match &value {
            Value::String { .. } => self.string_count += 1,
            Value::Function { .. } => self.function_count += 1,
            Value::Closure { .. } => self.closure_count += 1,
            Value::BuiltinFunction { .. } => self.builtin_function_count += 1,
            Value::List { .. } => self.list_count += 1,
            Value::Bool { .. } => self.bool_count += 1,
            Value::Int { .. } => self.int_count += 1,
            Value::Int64 { .. } => self.int64_count += 1,
            Value::Undefined => self.undefined_count += 1,
            Value::Void => self.void_count += 1,
        }

        let layout = Layout::new::<Value>();
        let allocated_error = self
            .allocator
            .allocate(layout);

        let allocated = allocated_error.map_err(HeapError::AllocationFailed)?
            .cast::<Value>();

        unsafe {
            allocated.as_ptr().write(value);
        }

        self.allocate_count = self.allocate_count + 1;
        Ok(Handle(allocated.addr().get()))
    }

    pub fn get_mut(&self, handle: Handle) -> Result<&mut Value, HeapError> {
        let addr = NonZero::new(handle.0)
            .ok_or_else(|| HeapError::PointerIsNull)?
            .get();
        unsafe {
            let value_ref = addr as *mut Value;
            value_ref.as_mut().ok_or_else(|| HeapError::PointerIsNull)
        }
    }

    pub fn get(&self, handle: Handle) -> Result<&Value, HeapError> {
        let addr = NonZero::new(handle.0)
            .ok_or_else(|| HeapError::PointerIsNull)?
            .get();
        unsafe {
            let value_ref = addr as *const Value;
            value_ref.as_ref().ok_or_else(|| HeapError::PointerIsNull)
        }
    }

    pub fn get_allocate_count(&self) -> usize {
        self.allocate_count
    }

    pub fn get_new_chunk_count(&self) -> usize {
        self.allocator.get_new_chunk_count()
    }

    pub fn get_bool_count(&self) -> usize {
        self.bool_count
    }

    pub fn get_int_count(&self) -> usize {
        self.int_count
    }

    pub fn get_int64_count(&self) -> usize {
        self.int64_count
    }

    pub fn get_string_count(&self) -> usize {
        self.string_count
    }

    pub fn get_function_count(&self) -> usize {
        self.function_count
    }

    pub fn get_closure_count(&self) -> usize {
        self.closure_count
    }

    pub fn get_builtin_function_count(&self) -> usize {
        self.builtin_function_count
    }

    pub fn get_list_count(&self) -> usize {
        self.list_count
    }

    pub fn get_undefined_count(&self) -> usize {
        self.undefined_count
    }

    pub fn get_void_count(&self) -> usize {
        self.void_count
    }
}
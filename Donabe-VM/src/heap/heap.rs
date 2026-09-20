use crate::allocator::bump_allocator::{BumpAllocator, DEFAULT_CHUNK_SIZE};
use crate::heap::handle::Handle;
use crate::heap::heap_error::HeapError;
use crate::value::Value;
use std::alloc::Layout;
use std::num::NonZero;

const INT_CACHE_SIZE: usize = 256;
const INT_CACHE_MIN: i32 = -(INT_CACHE_SIZE as i32 / 2);
const INT_CACHE_MAX: i32 = INT_CACHE_SIZE as i32 / 2 - 1;
const INT_CACHE_ADD: i32 = INT_CACHE_MIN.abs();

pub struct Heap {
    allocator: BumpAllocator,

    bool_true_handle: Handle,
    bool_false_handle: Handle,

    int_cache_handles: [Handle; INT_CACHE_SIZE],
}

impl Heap {
    pub fn new() -> Result<Heap, HeapError> {
        let mut allocator = BumpAllocator::new(DEFAULT_CHUNK_SIZE, align_of::<Value>())
            .map_err(HeapError::AllocationFailed)?;

        let bool_true_handle = alloc_impl(&mut allocator, Value::Bool { value: true })?;
        let bool_false_handle = alloc_impl(&mut allocator, Value::Bool { value: false })?;

        let mut int_cache_handles = [Handle(0); INT_CACHE_SIZE];

        for i in INT_CACHE_MIN..=INT_CACHE_MAX {
            let handle = alloc_impl(&mut allocator, Value::Int {value: i})?;
            int_cache_handles[(i + INT_CACHE_ADD) as usize] = handle;
        }

        Ok(Heap {
            allocator,

            bool_true_handle,
            bool_false_handle,

            int_cache_handles,
        })
    }

    #[inline(never)]
    pub fn alloc(&mut self, value: Value) -> Result<Handle, HeapError> {
        if let Value::Bool { value: bool_value } = value {
            return Ok(if bool_value {
                self.bool_true_handle
            } else {
                self.bool_false_handle
            });
        }
        if let Value::Int { value: int_value } = value &&
            INT_CACHE_MIN <= int_value && int_value <= INT_CACHE_MAX {
            return Ok(self.int_cache_handles[(int_value + INT_CACHE_ADD) as usize]);
        }

        alloc_impl(&mut self.allocator, value)
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
}

fn alloc_impl(allocator: &mut BumpAllocator, value: Value) -> Result<Handle, HeapError> {
    let layout = Layout::new::<Value>();
    let allocated_error = allocator.allocate(layout);

    let allocated = allocated_error
        .map_err(HeapError::AllocationFailed)?
        .cast::<Value>();

    unsafe {
        allocated.as_ptr().write(value);
    }
    Ok(Handle(allocated.addr().get()))
}

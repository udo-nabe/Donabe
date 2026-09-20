use crate::allocator::alloc_error::AllocationError;
use crate::allocator::chunk::Chunk;
use std::alloc::{Layout};
use std::cmp::max;
use std::ptr::NonNull;

pub const DEFAULT_CHUNK_SIZE: usize = 1024 * 1024;

pub struct BumpAllocator {
    current: Chunk,
    old_chunks: Vec<Chunk>,
}

impl BumpAllocator {
    pub fn new(chunk_size: usize, chunk_align: usize) -> Result<BumpAllocator, AllocationError> {
        Ok(BumpAllocator {
            current: Chunk::new(chunk_size, chunk_align)?,
            old_chunks: Vec::new(),
        })
    }

    pub fn allocate(&mut self, layout: Layout) -> Result<NonNull<u8>, AllocationError> {
        if let Some(ptr) = self.current.allocate(layout) {
            return Ok(ptr);
        }

        let new_chunk = Chunk::new(max(layout.size(), DEFAULT_CHUNK_SIZE), layout.align())?;
        let old_chunk = std::mem::replace(&mut self.current, new_chunk);
        self.old_chunks.push(old_chunk);
        Ok(self.current.allocate(layout).unwrap())
    }
}

#[test]
fn bump_allocator_allocates() {
    let mut allocator = BumpAllocator::new(128, 8).unwrap();

    let ptr = allocator.allocate(Layout::new::<u64>()).unwrap();

    assert_eq!(ptr.as_ptr() as usize % align_of::<u64>(), 0);
}

#[test]
fn bump_allocator_allocates_across_chunks() {
    let mut allocator = BumpAllocator::new(8, 8).unwrap();

    let ptr1 = allocator.allocate(Layout::new::<u64>()).unwrap();
    let ptr2 = allocator.allocate(Layout::new::<u64>()).unwrap();

    assert_ne!(ptr1.addr(), ptr2.addr());
    assert_eq!(allocator.old_chunks.len(), 1);
}

#[test]
fn bump_allocator_allocates_large_layout() {
    let mut allocator = BumpAllocator::new(8, 8).unwrap();

    let big_layout = Layout::from_size_align(8192, 8).unwrap();
    let ptr = allocator.allocate(big_layout).unwrap();

    assert_eq!(ptr.addr().get() % 8, 0);
}
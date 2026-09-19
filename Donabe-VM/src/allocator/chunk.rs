use crate::allocator::alloc_error::AllocationError;
use std::alloc::{Layout, alloc, dealloc};
use std::ptr::NonNull;

#[derive(Debug)]
pub struct Chunk {
    base: NonNull<u8>,
    capacity: usize,
    offset: usize,
    layout: Layout,
}

impl Chunk {
    pub fn new(capacity: usize, align: usize) -> Result<Chunk, AllocationError> {
        let layout =
            Layout::from_size_align(capacity, align).map_err(AllocationError::LayoutError)?;
        let base = match NonNull::new(unsafe { alloc(layout) }) {
            Some(v) => v,
            None => return Err(AllocationError::AllocError),
        };

        Ok(Chunk {
            base,
            capacity,
            offset: 0,
            layout,
        })
    }

    pub fn allocate(&mut self, layout: Layout) -> Option<NonNull<u8>> {
        let base_addr = self.base.addr().get();
        let align = layout.align();

        let current = base_addr + self.offset;
        let padding = (align - current % align) % align;
        let start = current + padding;
        let end = start + layout.size();

        if end > base_addr + self.capacity {
            return None;
        }

        self.offset = end - base_addr;
        NonNull::new(start as *mut u8)
    }
}

impl Drop for Chunk {
    fn drop(&mut self) {
        unsafe {
            dealloc(self.base.as_ptr(), self.layout);
        }
    }
}

#[test]
fn chunk_allocate_respects_alignment() {
    let mut chunk = Chunk::new(128, 8).unwrap();
    let ptr = chunk.allocate(Layout::new::<u64>()).unwrap();
    assert_eq!(ptr.as_ptr() as usize % align_of::<u64>(), 0);
}

#[test]
fn chunk_allocate_many_times() {
    let mut chunk = Chunk::new(128, 8).unwrap();
    let ptr1 = chunk.allocate(Layout::new::<u64>()).unwrap();
    let ptr2 = chunk.allocate(Layout::new::<u64>()).unwrap();
    let ptr3 = chunk.allocate(Layout::new::<u64>()).unwrap();
    let ptr4 = chunk.allocate(Layout::new::<u64>()).unwrap();

    assert!(ptr1.addr().get() + size_of::<u64>() <= ptr2.addr().get());
    assert!(ptr2.addr().get() + size_of::<u64>() <= ptr3.addr().get());
    assert!(ptr3.addr().get() + size_of::<u64>() <= ptr4.addr().get());
}

#[test]
fn chunk_allocate_exceeding_capacity() {
    let mut chunk = Chunk::new(4, 8).unwrap();
    let ptr = chunk.allocate(Layout::new::<u64>());
    assert!(ptr.is_none());
}

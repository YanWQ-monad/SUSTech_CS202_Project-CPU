//! This module provides the raw allocator and its support types.
//!
//! A "raw allocator" is one, that simply gets request for a specific memory
//! size but does not need to worry about alignment.
mod buffer;
mod entry;

use buffer::HEADER_SIZE;
use entry::{Entry, State};

use core::mem::MaybeUninit;

/// An error occurred when calling `free()`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FreeError {
    /// There is a double-free detected. An already freed-up-block is freed up
    /// again.
    DoubleFreeDetected,
    /// An invalid pointer was freed up (either a pointer outside of the heap
    /// memory or a pointer to a header).
    AllocationNotFound,
}

/// A raw memory allocator for contiguous slices of bytes without any alignment.
///
/// This allocator is an intermediate one, which does not need to handle the
/// alignment of a [`Layout`](core::alloc::Layout). This abstracts the parts
/// "allocating of memory" and "getting a pointer with proper alignment".
///
/// Note, that the allocated memory is always aligned to `4`.
pub struct RawAllocator<const N: usize> {
    /// The internal buffer abstracting over the raw bytes of the heap.
    buffer: buffer::Buffer<N>,
}
impl<const N: usize> RawAllocator<N> {
    /// Create a new [`RawAllocator`] with a given heap size.
    ///
    /// # Panics
    /// This function panics if the buffer size is less than `8` (the minimum
    /// useful allocation heap) or if it is not divisible by 4.
    pub const fn new() -> Self {
        assert!(N >= 8, "too small heap memory: minimum size is 8");
        assert!(N % 4 == 0, "memory size has to be divisible by 4");

        let buffer = buffer::Buffer::new();
        Self { buffer }
    }

    /// Allocate a new memory block of size `n`.
    ///
    /// This method is used for general allocation of multiple contiguous bytes.
    /// It searches for the smallest possible free entry and mark it as "used".
    /// As usual with [`RawAllocator`], this does not take alignment in account.
    ///
    /// If the allocation fails, `None` will be returned.
    pub fn alloc(&mut self, n: usize) -> Option<&mut [MaybeUninit<u8>]> {
        // round up `n` to next multiple of `size_of::<Entry>()`
        let n = (n + HEADER_SIZE - 1) / HEADER_SIZE * HEADER_SIZE;

        let (offset, _) = self
            .buffer
            .entries()
            .map(|offset| (offset, self.buffer[offset]))
            .filter(|(_offset, entry)| entry.state() == State::Free)
            .filter(|(_offset, entry)| entry.size() >= n)
            .min_by_key(|(_offset, entry)| entry.size())?;

        // if the found block is large enough, split it into a used and a free
        self.buffer.mark_as_used(offset, n);
        Some(self.buffer.memory_of_mut(offset))
    }

    /// Free a pointer inside a used memory block.
    ///
    /// This method is used to release a memory block allocated with this raw
    /// allocator. If a entry to the given pointer is found, the corresponding
    /// memory block is marked as free. If no entry is found, than an error is
    /// reported (as allocators are not allowed to unwind).
    ///
    /// # Algorithm
    /// Freeing a pointer is done in the following way: all the entries are
    /// scanned linearly. The pointer is compared against each block. If the
    /// pointer points to the memory of an entry, than that entry is selected.
    /// If no such entry is found, than the user tried to free an allocation,
    /// that was not allocated with this allocator (or the allocator messed up
    /// internally). [`FreeError::AllocationNotFound`] is reported.
    ///
    /// The selected block is tested for its state. If it is marked as "used",
    /// than everything is fine. If it is already marked as "free", than
    /// [`FreeError::DoubleFreeDetected`] is returned. If the block following
    /// the just freed up one is also free, the two blocks are concatenated to a
    /// single one (to prevent fragmentation).
    pub fn free(&mut self, ptr: *mut u8) -> Result<(), FreeError> {
        let offset = self
            .buffer
            .entries()
            .find(|offset| {
                let size = self.buffer[*offset].size();
                let memory = self.buffer.memory_of(*offset);
                let ptr = ptr as *const _;
                let start = memory.as_ptr();
                let end = start.wrapping_add(size);

                start <= ptr && ptr < end
            })
            .ok_or(FreeError::AllocationNotFound)?;

        let entry = self.buffer[offset];
        if entry.state() == State::Free {
            return Err(FreeError::DoubleFreeDetected);
        }
        let additional_memory = self
            .buffer
            .following_free_entry(offset)
            .map_or(0, |entry| entry.size() + HEADER_SIZE);
        self.buffer[offset] = Entry::free(entry.size() + additional_memory);
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::{Entry, FreeError, RawAllocator};

    /// Test, that the given allocator has exactly the given entries.
    macro_rules! assert_allocations {
        ($allocator:expr, $($entry:expr),*$(,)?) => {{
            let mut iter = $allocator
                .buffer
                .entries()
                .map(|offset| $allocator.buffer[offset]);
            $(assert_eq!(iter.next(), Some($entry));)*
            assert_eq!(iter.next(), None);
        }};
    }

    #[test]
    fn successful_single_allocation() {
        let mut allocator = RawAllocator::<32>::new();
        allocator.alloc(4).unwrap();
        assert_allocations!(allocator, Entry::used(4), Entry::free(20));
    }

    #[test]
    fn unsuccessful_single_allocation() {
        // the allocation is larger than the buffer itself
        let mut allocator = RawAllocator::<32>::new();
        assert!(allocator.alloc(36).is_none());
        assert_allocations!(allocator, Entry::free(28));
    }

    #[test]
    fn successful_multiple_allocation() {
        let mut allocator = RawAllocator::<32>::new();
        allocator.alloc(12).unwrap();
        allocator.alloc(12).unwrap();
        // allocator is now full
        assert_allocations!(allocator, Entry::used(12), Entry::used(12));
    }

    #[test]
    fn unsuccessful_multiple_allocation() {
        let mut allocator = RawAllocator::<32>::new();
        allocator.alloc(12).unwrap();
        // the second allocation is larger than the remaining space
        assert!(allocator.alloc(13).is_none());
        assert_allocations!(allocator, Entry::used(12), Entry::free(12));
    }

    macro_rules! address {
        ($memory:expr) => {
            $memory.as_mut_ptr().cast::<u8>()
        };
    }

    #[test]
    fn unsuccessful_allocation_due_to_fragmentation() {
        // this test case shows, that the allocator is susceptible to memory
        // fragmentation, which makes larger allocations impossible, if the
        // heap is in a bad state.
        let mut allocator = RawAllocator::<60>::new();

        // build a fragmented heap
        let ptr1 = address!(allocator.alloc(8).unwrap());
        let _ptr2 = address!(allocator.alloc(8).unwrap());
        let ptr3 = address!(allocator.alloc(8).unwrap());
        let _ptr4 = address!(allocator.alloc(8).unwrap());
        let ptr5 = address!(allocator.alloc(8).unwrap());
        allocator.free(ptr1).unwrap();
        allocator.free(ptr3).unwrap();
        allocator.free(ptr5).unwrap();
        assert_allocations!(
            allocator,
            Entry::free(8),
            Entry::used(8),
            Entry::free(8),
            Entry::used(8),
            Entry::free(8)
        );

        // now, there are 24 free bytes (3x 8 bytes) and the headers, but the
        // allocation of 10 bytes must fail, since there is no contiguous memory
        // of that size
        assert!(allocator.alloc(10).is_none());
    }

    #[test]
    fn simple_free() {
        let mut allocator = RawAllocator::<16>::new();
        let ptr = address!(allocator.alloc(4).unwrap());
        allocator.alloc(4).unwrap();
        assert_allocations!(allocator, Entry::used(4), Entry::used(4));

        // now, that the heap is properly built up, there are two used entries.
        // when the first one is freed up, there is no possibility for merging
        // the free memory with the following one (as that one is used)
        allocator.free(ptr).unwrap();
        assert_allocations!(allocator, Entry::free(4), Entry::used(4));
    }

    #[test]
    fn double_free() {
        let mut allocator = RawAllocator::<16>::new();
        let ptr = address!(allocator.alloc(4).unwrap());
        allocator.alloc(4).unwrap();

        // try to free up the pointer twice. The first time has to succeed, but
        // the second time has to result in a double-free-error.
        allocator.free(ptr).unwrap();
        assert_eq!(allocator.free(ptr), Err(FreeError::DoubleFreeDetected));
        assert_allocations!(allocator, Entry::free(4), Entry::used(4));
    }

    #[test]
    fn invalid_free() {
        use core::ptr;

        let mut allocator = RawAllocator::<32>::new();
        allocator.alloc(4).unwrap();

        // try to free up a pointer, that was not allocated by this allocator.
        // This invalid usage has to be detected.
        let mut x = 0_u32;
        let ptr = ptr::addr_of_mut!(x).cast();
        assert_eq!(allocator.free(ptr), Err(FreeError::AllocationNotFound));
    }

    #[test]
    fn free_of_modified_pointer() {
        let mut allocator = RawAllocator::<16>::new();
        let ptr = address!(allocator.alloc(4).unwrap());
        allocator.alloc(4).unwrap();
        assert_allocations!(allocator, Entry::used(4), Entry::used(4));

        let ptr = ptr.wrapping_add(3);
        // now there is a valid pointer, but this pointer was modified, e.g. to
        // be aligned properly. As the `free()`-call should support any pointer
        // into the memory block, this should succeed.
        allocator.free(ptr).unwrap();
        assert_allocations!(allocator, Entry::free(4), Entry::used(4));
    }

    #[test]
    fn free_with_concatenation() {
        let mut allocator = RawAllocator::<32>::new();
        let ptr = address!(allocator.alloc(4).unwrap());
        assert_allocations!(allocator, Entry::used(4), Entry::free(20));

        // now there is a used block followed by a free block. When the used
        // block is freed up as well, this should lead to a single free block.
        allocator.free(ptr).unwrap();
        assert_allocations!(allocator, Entry::free(28));
    }

    #[test]
    fn free_at_end() {
        let mut allocator = RawAllocator::<16>::new();
        allocator.alloc(4).unwrap();
        let ptr = address!(allocator.alloc(4).unwrap());
        assert_allocations!(allocator, Entry::used(4), Entry::used(4));

        // now, that the heap is properly built up, there are two used entries.
        // when the second one is freed up, there is no possibility for merging
        // the free memory with the following one (as there is no following one)
        allocator.free(ptr).unwrap();
        assert_allocations!(allocator, Entry::used(4), Entry::free(4));
    }

    #[test]
    fn free_impossible_defrag() {
        let mut allocator = RawAllocator::<16>::new();
        let ptr1 = address!(allocator.alloc(4).unwrap());
        let ptr2 = address!(allocator.alloc(4).unwrap());
        allocator.free(ptr1).unwrap();

        // now we have a free block, followed by a used block which in turn gets
        // freed up. Therefore there are two contiguous free blocks, but those
        // aren't concatenated, since the old free block is to the left (instead
        // of to the right).
        allocator.free(ptr2).unwrap();

        // therefore there must be two free blocks
        assert_allocations!(allocator, Entry::free(4), Entry::free(4));
    }

    #[test]
    fn alloc_impossible_splitting() {
        let mut allocator = RawAllocator::<32>::new();
        let _ptr1 = address!(allocator.alloc(4).unwrap());
        let ptr2 = address!(allocator.alloc(12).unwrap());
        let _ptr3 = address!(allocator.alloc(4).unwrap());
        allocator.free(ptr2).unwrap();
        assert_allocations!(allocator, Entry::used(4), Entry::free(12), Entry::used(4));

        // new we've set up the heap such there is a free block of 12 in the
        // middle (and no free data at the end). If one acquires a block of size
        // 4 everything should work fine and the free block should be split up.;
        let ptr4 = address!(allocator.alloc(4).unwrap());
        assert_allocations!(
            allocator,
            Entry::used(4),
            Entry::used(4),
            Entry::free(4),
            Entry::used(4)
        );
        allocator.free(ptr4).unwrap();
        assert_allocations!(allocator, Entry::used(4), Entry::free(12), Entry::used(4));

        // now the previous state is restored. If there is an allocation for a
        // size of 12, no splitting must be happening, since the block is only
        // 12 bytes of size, so splitting would tamper the following block.
        let _ptr5 = address!(allocator.alloc(12).unwrap());
        assert_allocations!(allocator, Entry::used(4), Entry::used(12), Entry::used(4));
    }

    #[test]
    fn free_error_properties() {
        // pointless and rather dumb test case: check, that the derived traits
        // work as expected.
        use super::FreeError::{AllocationNotFound, DoubleFreeDetected};

        assert_eq!(AllocationNotFound, AllocationNotFound);
        assert_eq!(DoubleFreeDetected, DoubleFreeDetected);
        assert_ne!(AllocationNotFound, DoubleFreeDetected);

        assert_eq!(AllocationNotFound.clone(), AllocationNotFound);
        assert_eq!(DoubleFreeDetected.clone(), DoubleFreeDetected);
        assert_ne!(AllocationNotFound.clone(), DoubleFreeDetected);

        assert_eq!(format!("{:?}", AllocationNotFound), "AllocationNotFound");
        assert_eq!(format!("{:?}", DoubleFreeDetected), "DoubleFreeDetected");
    }
}

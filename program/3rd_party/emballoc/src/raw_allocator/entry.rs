//! This module exposes the ubiquitous [`Entry`] type and its helper [`State`].
#[cfg(test)]
use core::fmt::{self, Debug, Formatter};

/// Helper for showing, whether a block is used or freed.
///
/// This primarily exists to be able to match on the block state and to work
/// ergonomically with it.
#[derive(Debug, Clone, Copy, Eq, PartialEq)]
pub enum State {
    /// The entry is marked as "used".
    ///
    /// The memory after the header is assumed to contain used data and must not
    /// be re-used for other allocations.
    Free,
    /// The entry is marked as "free".
    ///
    /// The memory after the header is assumed to be free and thus can be used
    /// for a new allocation.
    Used,
}

/// An (un)allocated block.
///
/// Such a block is either used or free and contains a block size. It is a
/// compact version of the following `enum`:
/// ```
/// # use core::mem;
/// enum NaiveEntry {
///     Used(u32),
///     Free(u32),
/// }
///
/// // mem::size_of::<Entry>() is not possible in doc-tests, since the struct is not exposed
/// assert!(mem::size_of::<NaiveEntry>() > mem::size_of::<u32>());
/// ```
/// This struct is used heavily in the allocator and thus has to be optimized.
/// It is used as a header for blocks of allocated or unallocated memory inside
/// the heap buffer. Entries are written directly into the buffer, therefore
/// their layout is important.
#[repr(transparent)]
#[derive(Clone, Copy, PartialEq, Eq)]
pub struct Entry(u32);
impl Entry {
    /// Create a new free [`Entry`] with the given size.
    ///
    /// Note, that the `size` is the number of bytes of the payload, i.e. the
    /// size after the `Entry` itself. This is the same value as returned by
    /// [`size()`](Entry::size()).
    pub const fn free(size: usize) -> Self {
        assert!(size <= 0x7FFF_FFFF);
        #[allow(clippy::cast_possible_truncation)] // asserted above
        Self((size << 1) as _)
    }

    /// Create a new occupied/used [`Entry`] with the given size.
    ///
    /// Note, that the `size` is the number of bytes of the payload, i.e. the
    /// size after the `Entry` itself. This is the same value as returned by
    /// [`size()`](Entry::size()).
    pub const fn used(size: usize) -> Self {
        assert!(size <= 0x7FFF_FFFF);
        #[allow(clippy::cast_possible_truncation)] // asserted above
        Self((size << 1 | 1) as _)
    }

    /// Query the allocation state of this block.
    pub const fn state(self) -> State {
        if self.0 & 1 == 0 {
            State::Free
        } else {
            State::Used
        }
    }

    /// Query the size of the block.
    ///
    /// This is the size of the usable memory, i.e. the header size is not
    /// included.
    pub const fn size(self) -> usize {
        let size = self.0 >> 1;
        size as _
    }

    /// Query the raw bytes of this entry in native endian order.
    pub const fn as_raw(self) -> [u8; 4] {
        self.0.to_ne_bytes()
    }
}
#[cfg(test)]
impl Debug for Entry {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        f.debug_struct("Entry")
            .field("state", &self.state())
            .field("size", &self.size())
            .finish()
    }
}

#[cfg(test)]
mod tests {
    use super::{Entry, State};

    #[test]
    fn equality() {
        assert_eq!(Entry::used(4), Entry::used(4));
        assert_ne!(Entry::used(4), Entry::used(5));

        assert_eq!(Entry::free(4), Entry::free(4));
        assert_ne!(Entry::free(4), Entry::free(5));

        assert_ne!(Entry::used(4), Entry::free(4));
        assert_ne!(Entry::used(4), Entry::free(5));

        // now same with cloning
        assert_eq!(Entry::used(4).clone(), Entry::used(4));
        assert_ne!(Entry::used(4).clone(), Entry::used(5));

        assert_eq!(Entry::free(4).clone(), Entry::free(4));
        assert_ne!(Entry::free(4).clone(), Entry::free(5));

        assert_ne!(Entry::used(4).clone(), Entry::free(4));
        assert_ne!(Entry::used(4).clone(), Entry::free(5));
    }

    #[test]
    fn entry_bitpacking_state() {
        assert_eq!(Entry::free(5).state(), State::Free);
        assert_eq!(Entry::used(5).state(), State::Used);

        assert_eq!(Entry(0b00_0).state(), State::Free);
        assert_eq!(Entry(0b00_1).state(), State::Used);

        assert_eq!(Entry(0b10_0).state(), State::Free);
        assert_eq!(Entry(0b10_1).state(), State::Used);
        assert_eq!(Entry(0b11_1).state(), State::Used);
        assert_eq!(Entry(0b11_0).state(), State::Free);

        // now the same with cloning
        assert_eq!(Entry::free(5).state().clone(), State::Free);
        assert_eq!(Entry::used(5).state().clone(), State::Used);

        assert_eq!(Entry(0b00_0).state().clone(), State::Free);
        assert_eq!(Entry(0b00_1).state().clone(), State::Used);

        assert_eq!(Entry(0b10_0).state().clone(), State::Free);
        assert_eq!(Entry(0b10_1).state().clone(), State::Used);
        assert_eq!(Entry(0b11_1).state().clone(), State::Used);
        assert_eq!(Entry(0b11_0).state().clone(), State::Free);
    }

    #[test]
    fn entry_bitpacking_size() {
        assert_eq!(Entry(0b1_1).size(), 1);
        assert_eq!(Entry(0b1_0).size(), 1);
        assert_eq!(Entry(123 << 1).size(), 123);
        assert_eq!(Entry(123 << 1 | 1).size(), 123);
    }

    #[test]
    fn alignment() {
        use core::mem;

        // other parts of this crate might assume, that this type has the same
        // alignment as a `u32`, i.e. `4`
        assert_eq!(mem::align_of::<Entry>(), mem::align_of::<u32>());
    }

    #[test]
    fn large_entries() {
        Entry::free((1 << 31) - 4);
        Entry::used((1 << 31) - 4);
    }

    #[test]
    #[should_panic]
    fn huge_free_block() {
        Entry::free(1 << 31); // panic here
    }

    #[test]
    #[should_panic]
    fn huge_used_block() {
        Entry::used(1 << 31); // panic here
    }

    #[test]
    fn debug_representation() {
        assert_eq!(
            format!("{:?}", Entry::used(123)),
            "Entry { state: Used, size: 123 }"
        );
        assert_eq!(
            format!("{:?}", Entry::free(456)),
            "Entry { state: Free, size: 456 }"
        );
    }
}

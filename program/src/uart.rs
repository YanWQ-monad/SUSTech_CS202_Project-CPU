use crate::board::*;

#[inline]
pub fn output_ready() -> bool {
    unsafe { read_u32(0xFFFFF00C) > 0 }
}

#[inline]
pub fn input_ready() -> bool {
    unsafe { read_u32(0xFFFFF008) > 0 }
}

#[inline]
pub unsafe fn write_unchecked(data: u8) {
    unsafe { write_u32(0xFFFFF004, data as u32) }
}

#[inline]
pub unsafe fn read_unchecked() -> u8 {
    unsafe { read_u32(0xFFFFF000) as u8 }
}

#[inline]
pub fn write(data: u8) {
    while !output_ready() {}
    unsafe { write_unchecked(data) }
}

#[inline]
pub fn read() -> u8 {
    while !input_ready() {}
    unsafe { read_unchecked() }
}

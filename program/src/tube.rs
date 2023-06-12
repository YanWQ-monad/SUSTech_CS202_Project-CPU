use crate::board::*;

#[repr(u8)]
pub enum TubeMode {
    Dec = 0,
    Hex = 1,
}

pub fn set_tube_value(v: u32) {
    unsafe {
        write_u32(0xFFFFF038, v);
    }
}

pub fn set_tube_enable(mask: u8) {
    unsafe {
        write_u32(0xFFFFF03C, mask as u32);
    }
}

pub fn set_tube_mode(mode: TubeMode) {
    unsafe {
        write_u32(0xFFFFF030, mode as u8 as u32);
    }
}

pub fn set_tube_value_option(v: Option<u32>) {
    set_tube_enable(if v.is_some() { 0xFF } else { 0 });
    if let Some(v) = v {
        set_tube_value(v);
    }
}

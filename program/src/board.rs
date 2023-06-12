use core::ptr::{read_volatile, write_volatile};

const SWITCH_BASEADDR: *mut u32 = 0xFFFFF100 as *mut u32;
const LED_BASEADDR: *mut u32 = 0xFFFFF200 as *mut u32;

#[derive(Debug)]
#[repr(u8)]
pub enum Keyboard {
    _0 = 2,
    _1 = 15,
    _2 = 14,
    _3 = 13,
    _4 = 11,
    _5 = 10,
    _6 = 9,
    _7 = 7,
    _8 = 6,
    _9 = 5,
    A = 12,
    B = 8,
    C = 4,
    D = 0,
    Star = 3,
    Number = 1,
}

#[repr(usize)]
pub enum Button {
    Center = 0xFFFFF01C,
    Up = 0xFFFFF020,
    Down = 0xFFFFF024,
    Left = 0xFFFFF028,
    Right = 0xFFFFF02C,
}

impl Keyboard {
    pub fn as_number(&self) -> Option<u32> {
        match self {
            Keyboard::_0 => Some(0),
            Keyboard::_1 => Some(1),
            Keyboard::_2 => Some(2),
            Keyboard::_3 => Some(3),
            Keyboard::_4 => Some(4),
            Keyboard::_5 => Some(5),
            Keyboard::_6 => Some(6),
            Keyboard::_7 => Some(7),
            Keyboard::_8 => Some(8),
            Keyboard::_9 => Some(9),
            _ => None,
        }
    }
}

#[inline]
pub unsafe fn read_u32(addr: usize) -> u32 {
    read_volatile(addr as *mut u32)
}

#[inline]
pub unsafe fn write_u32(addr: usize, data: u32) {
    write_volatile(addr as *mut u32, data)
}

pub fn read_switch(index: usize) -> bool {
    assert!(index < 24);
    unsafe { SWITCH_BASEADDR.add(index).read_volatile() > 0 }
}

pub fn set_led(index: usize, enable: bool) {
    assert!(index < 24);
    unsafe { LED_BASEADDR.add(index).write_volatile(enable as u32) }
}

pub fn read_button(button: Button) -> bool {
    unsafe { read_u32(button as usize) > 0 }
}

pub fn random_value() -> u32 {
    unsafe { read_u32(0xFFFFF044) }
}

pub fn keyboard_ready() -> bool {
    unsafe { read_u32(0xFFFFF018) > 0 }
}

pub fn read_keyboard() -> Keyboard {
    while !keyboard_ready() {}
    unsafe { core::mem::transmute(read_u32(0xFFFFF014) as u8) }
}

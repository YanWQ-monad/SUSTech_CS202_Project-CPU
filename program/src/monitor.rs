const DATA_BASEADDR: *mut u32 = 0xFFFD0000 as *mut u32;
const COLOR_BASEADDR: *mut u32 = 0xFFFE0000 as *mut u32;

pub const SCREEN_BUFFER_MULTIPLIER: usize = 256;
pub const SCREEN_WIDTH: usize = 800 / 8;
pub const SCREEN_HEIGHT: usize = 600 / 16;

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Color {
    Black = 0x0,
    Red = 0x1,
    Green = 0x2,
    Yellow = 0x3,
    Blue = 0x4,
    Magenta = 0x5,
    Cyan = 0x6,
    White = 0x7,
    BrightBlack = 0x8,
    BrightRed = 0x9,
    BrightGreen = 0xA,
    BrightYellow = 0xB,
    BrightBlue = 0xC,
    BrightMagenta = 0xD,
    BrightCyan = 0xE,
    BrightWhite = 0xF,
}

impl Color {
    pub fn default_fg() -> Color {
        Color::White
    }
    pub fn default_bg() -> Color {
        Color::Black
    }
}

pub fn init() {
    monitor::set_color(0x07);
}

#[inline]
fn offset(x: usize, y: usize) -> usize {
    // assert!(x < SCREEN_BUFFER_MULTIPLIER && y < 64);
    y * SCREEN_BUFFER_MULTIPLIER + x
}

pub fn set_character(x: usize, y: usize, ch: u8) {
    unsafe { DATA_BASEADDR.add(offset(x, y)).write_volatile(ch as u32) }
}

pub fn get_character(x: usize, y: usize) -> u8 {
    unsafe { DATA_BASEADDR.add(offset(x, y)).read_volatile() as u8 }
}

pub fn set_color(x: usize, y: usize, ch: u8) {
    unsafe { COLOR_BASEADDR.add(offset(x, y)).write_volatile(ch as u32) }
}

pub fn get_color(x: usize, y: usize) -> u8 {
    unsafe { COLOR_BASEADDR.add(offset(x, y)).read_volatile() as u8 }
}

static mut SCREEN_X: usize = 0;
static mut SCREEN_Y: usize = 0;
static mut CURRENT_COLOR: u8 = 0;

pub mod monitor {
    use super::*;

    #[inline]
    pub fn get_x() -> usize {
        unsafe { SCREEN_X }
    }

    #[inline]
    pub fn get_y() -> usize {
        unsafe { SCREEN_Y }
    }

    #[inline]
    pub fn set_x(x: usize) {
        unsafe { SCREEN_X = x }
    }

    #[inline]
    pub fn set_y(y: usize) {
        unsafe { SCREEN_Y = y }
    }

    #[inline]
    pub fn set_xy(x: usize, y: usize) {
        unsafe {
            SCREEN_X = x;
            SCREEN_Y = y;
        }
    }

    #[inline]
    pub fn get_color() -> u8 {
        unsafe { CURRENT_COLOR }
    }

    #[inline]
    pub fn set_color(color: u8) {
        unsafe { CURRENT_COLOR = color }
    }

    pub fn clear_screen() {
        let color = get_color();
        for y in 0..SCREEN_HEIGHT {
            for x in 0..SCREEN_WIDTH {
                set_character(x, y, b' ');
                super::set_color(x, y, color);
            }
        }
        set_xy(0, 0);
    }

    pub fn scroll_down() {
        for y in 0..SCREEN_HEIGHT - 1 {
            for x in 0..SCREEN_WIDTH {
                set_character(x, y, get_character(x, y + 1));
                super::set_color(x, y, super::get_color(x, y + 1));
            }
        }
        for x in 0..SCREEN_WIDTH {
            set_character(x, SCREEN_HEIGHT - 1, b' ');
        }
    }

    pub fn newline() {
        let y = get_y();
        if y + 1 == SCREEN_HEIGHT {
            scroll_down();
            set_x(0);
        } else {
            set_xy(0, y + 1);
        }
    }

    pub fn move_cursor_next() {
        let x = get_x();
        if x + 1 == SCREEN_WIDTH {
            newline();
        } else {
            set_x(x + 1);
        }
    }

    #[allow(irrefutable_let_patterns)]
    pub fn putchar(ch: u8) {
        match ch {
            b'\r' => set_x(0),
            b'\n' => newline(),
            b'\x7f' => if let x = get_x() && x > 0 { set_x(x - 1) },
            ch => {
                let x = get_x();
                let y = get_y();
                set_character(x, y, ch);
                super::set_color(x, y, get_color());
                move_cursor_next();
            }
        }
    }
}

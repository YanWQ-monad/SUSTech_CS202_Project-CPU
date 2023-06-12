use core::fmt::{self, Write};

use crate::monitor;
use crate::uart;

struct Stdout;

const UART_NEWLINE: u8 = b'\r';
const UART_BACKSPACE: u8 = b'\x7f';

static mut PRINT_TO_SCREEN: bool = true;

fn putchar(ch: u8) {
    uart::write(ch);
    if unsafe { PRINT_TO_SCREEN } {
        monitor::monitor::putchar(ch);
    }
}

pub fn set_screen_print(enable: bool) {
    unsafe { PRINT_TO_SCREEN = enable }
}

impl Write for Stdout {
    fn write_str(&mut self, s: &str) -> fmt::Result {
        for c in s.chars() {
            putchar(c as u8);
        }
        Ok(())
    }
}

pub fn print_str(s: &str) {
    Stdout.write_str(s);
}

pub fn print(args: fmt::Arguments) {
    Stdout.write_fmt(args).unwrap();
}

#[macro_export]
macro_rules! print {
    ($fmt: literal $(, $($arg: tt)+)?) => {
        $crate::prelude::print(format_args!($fmt $(, $($arg)+)?))
    }
}

#[macro_export]
macro_rules! println {
    ($fmt: literal $(, $($arg: tt)+)?) => {
        $crate::prelude::print(format_args!(concat!($fmt, "\r\n") $(, $($arg)+)?))
    }
}

pub fn get_char(echo: bool) -> u8 {
    let c = uart::read();
    if echo {
        putchar(c);
        match c {
            UART_NEWLINE => putchar(b'\n'),
            UART_BACKSPACE => {
                putchar(b'\x08');
                putchar(b' ');
                putchar(b'\x08');
            }
            _ => {}
        }
    }
    c
}

pub fn read_line(buf: &mut [u8]) -> &str {
    let mut idx: usize = 0;

    loop {
        match get_char(true) {
            UART_NEWLINE => break,
            UART_BACKSPACE => {
                idx = idx.saturating_sub(1);
            }
            c => {
                buf[idx] = c;
                idx += 1;
            }
        }
    }

    unsafe { core::str::from_utf8_unchecked(&buf[..idx]) }
}

#![no_std]
#![no_main]

use core::ptr::write_volatile;

extern crate cpu_lib;

use cpu_lib::prelude::*;

const APP_BASE_ADDRESS: usize = 0x00008000;

#[no_mangle]
fn main() -> i32 {
    print_str("[loader] Welcome to the program loader!\r\n");
    let mut buf = [0; 16];

    loop {
        print_str("[loader] Program size (bytes): ");
        let line = read_line(&mut buf);

        let size: usize = if let Ok(size) = line.parse() {
            size
        } else {
            print_str("[loader] Invalid number!");
            continue
        };

        print_str("[loader] Transfer the data below\r\n");
        unsafe {
            core::slice::from_raw_parts_mut(APP_BASE_ADDRESS as *mut u8, size)
                .iter_mut()
                .for_each(|addr| write_volatile(addr, get_char(false)));
        }

        print_str("[loader] Program loaded! Calling...\r\n");
        let code: extern "C" fn() = unsafe { core::mem::transmute(APP_BASE_ADDRESS as *const ()) };
        (code)();

        monitor::clear_screen();
        print_str("[loader] Program exited\r\n\r\n");
    }
}

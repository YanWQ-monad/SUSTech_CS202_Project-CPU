#![no_std]
#![feature(let_chains)]
#![feature(linkage)]
#![feature(panic_info_message)]

#[cfg(feature = "alloc")]
extern crate alloc;

use core::arch::global_asm;

pub mod board;
pub mod console;
mod lang_items;
pub mod monitor;
pub mod rng;
pub mod time;
pub mod tube;
pub mod uart;

#[cfg(feature = "alloc")]
mod allocator;

pub mod prelude;

// #[cfg(feature = "loader")]
// global_asm!(include_str!("entry-loader.asm"));
// #[cfg(not(feature = "loader"))]
global_asm!(include_str!("entry.asm"));

#[linkage = "weak"]
#[no_mangle]
fn main() -> i32 {
    panic!("Cannot find main!");
}

#[no_mangle]
#[link_section = ".text.entry"]
pub extern "C" fn rust_main() -> i32 {
    clear_bss();
    monitor::init();
    monitor::monitor::clear_screen();
    main()
}

#[allow(dead_code)]
fn clear_bss() {
    extern "C" {
        fn sbss();
        fn ebss();
    }

    (sbss as usize..ebss as usize).for_each(|a| unsafe { (a as *mut u8).write_volatile(0) });
}

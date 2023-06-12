#![no_std]
#![no_main]

extern crate alloc;

#[macro_use]
extern crate cpu_lib;

use game::logic::End;
use tetrs::Tetrs;

mod game;
mod input;
mod tetrs;
mod ui;

#[no_mangle]
fn main() -> i32 {
    let game = Tetrs::new();
    while game.run() != End::Quit {}

    0
}

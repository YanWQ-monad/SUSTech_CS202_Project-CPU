#![no_std]
#![no_main]

#[macro_use]
extern crate cpu_lib;

use cpu_lib::prelude::*;

fn read_number() -> u32 {
    let mut v = 0;
    let mut en = 0;

    while keyboard_ready() {
        read_keyboard();
    }

    loop {
        while !keyboard_ready() {
            if read_button(Button::Left) {
                en = en >> 1;
                v = v / 10;
                set_tube_value_option(Some(v));
                set_tube_enable(en);
                while read_button(Button::Left) {}
            }
            if read_button(Button::Center) {
                while read_button(Button::Center) {}
                return v;
            }
        }

        en = en << 1 | 1;
        let key = read_keyboard().as_number().unwrap();
        v = v * 10 + key;
        set_tube_value_option(Some(v));
        set_tube_enable(en);
    }
}

#[no_mangle]
fn main() -> i32 {
    let v = read_number();
    println!("read number => {}", v);

    0
}

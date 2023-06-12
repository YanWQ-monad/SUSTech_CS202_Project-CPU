#![no_std]
#![no_main]

#[macro_use]
extern crate cpu_lib;

use cpu_lib::prelude::*;

fn read_switch_number() -> u8 {
    let mut v = 0;
    for i in 0..8 {
        v = v << 1 | (read_switch(7 - i) as u8);
    }
    v
}

fn set_led_number(v: u8) {
    for i in 0..8 {
        set_led(i, (v & (1 << i)) > 0);
    }
}

#[no_mangle]
fn main() -> i32 {
    loop {
        println!("new task!");

        while !read_button(Button::Center) {}

        let b: u32 = read_switch(16) as u32
            | ((read_switch(17) as u32) << 1)
            | ((read_switch(18) as u32) << 2);
        println!("b = {}", b);

        (0..8).for_each(|x| set_led(x + 16, false));
        set_led(16 + b as usize, true);

        while read_button(Button::Center) {}
        while !read_button(Button::Center) {}

        let v1 = read_switch_number();
        set_led_number(v1);
        println!("v1 = {}", v1);

        while read_button(Button::Center) {}
        while !read_button(Button::Center) {}

        let v2 = if b > 1 { read_switch_number() } else { 0 };
        set_led_number(v2);
        println!("v2 = {}", v2);

        while read_button(Button::Center) {}
        while !read_button(Button::Center) {}

        let r = match b {
            0b000 => {
                let a = v1 as i32;
                ((a & (a - 1)) == 0) as u8
            }
            0b001 => v1 % 2,
            0b010 => v1 | v2,
            0b011 => !(v1 | v2),
            0b100 => v1 ^ v2,
            0b101 => ((v1 as i8) < (v2 as i8)) as u8,
            0b110 => (v1 < v2) as u8,
            0b111 => {
                println!("show v1 = {}", v1);
                set_tube_value_option(Some(v1 as u32));

                while read_button(Button::Center) {}
                while !read_button(Button::Center) {}

                v2
            }
            _ => 0,
        };
        set_led_number(r);
        set_tube_value_option(Some(r as u32));
        println!("r = {}", r);

        while read_button(Button::Center) {}
    }
}

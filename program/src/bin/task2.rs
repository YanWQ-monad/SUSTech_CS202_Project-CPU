#![no_std]
#![no_main]

use core::time::Duration;

#[macro_use]
extern crate cpu_lib;

use cpu_lib::prelude::*;

/*
fn read_number() -> u32 {
    let mut v = 0;
    let mut en = 0;

    while keyboard_ready() {
        read_keyboard();
    }

    loop {
        while !keyboard_ready() {
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
*/

fn read_number() -> u8 {
    while !read_button(Button::Center) {}

    let mut v = 0;
    for i in 0..8 {
        v = v << 1 | (read_switch(7 - i) as u8);
    }
    set_led_number(v);

    while read_button(Button::Center) {}

    v
}

fn set_led_number(v: u8) {
    for i in 0..8 {
        set_led(i, (v & (1 << i)) > 0);
    }
}

fn accumulate(v: u8, t: u8) -> u8 {
    if t == 1 {
        set_tube_value_option(Some(v as u32));
        sleep(Duration::from_secs(1));
    }

    match v {
        0 => 0,
        _ => {
            let w = accumulate(v - 1, t) + v;
            if t == 2 {
                set_tube_value_option(Some(v as u32));
                sleep(Duration::from_secs(1));
            }
            w
        }
    }
}

fn task_accumulative(b: u8) {
    let v1 = read_number();
    if (v1 as i8) < 0 {
        loop {
            set_led_number(v1);
            sleep(Duration::from_millis(500));
            set_led_number(0);
            sleep(Duration::from_millis(500));
        }
    }
    let w = accumulate(v1, b - 1);
    if b == 0 {
        println!("{}", w);
        set_tube_value_option(Some(w as u32));
    } else if b == 1 {
        set_tube_value_option(Some((v1 * 2) as u32));
    }
}

fn task_arithmetic(b: u8) {
    let v1 = read_number() as i8;
    let v2 = read_number() as i8;

    match b {
        0b100 => {
            let (r, o) = v1.overflowing_add(v2);
            set_led_number(r as u8);
            set_led(11, o);
        }
        0b101 => {
            let (r, o) = v1.overflowing_sub(v2);
            set_led_number(r as u8);
            set_led(11, o);
        }
        0b110 => {
            let r = ((v1 as i16) * (v2 as i16)) as u16;
            for i in 0..16 {
                set_led(i, (r & (1 << i)) > 0);
            }
        }
        0b111 => {
            let q = v1 / v2;
            let r = v1 % v2;
            loop {
                set_led_number(q as u8);
                sleep(Duration::from_secs(5));
                set_led_number(r as u8);
                sleep(Duration::from_secs(5));
            }
        }
        _ => {}
    }
}

#[no_mangle]
fn main() -> i32 {
    loop {
        println!("new task!");

        while !read_button(Button::Center) {}

        let b: u8 =
            read_switch(16) as u8 | ((read_switch(17) as u8) << 1) | ((read_switch(18) as u8) << 2);
        (0..8).for_each(|x| set_led(x + 16, false));
        set_led(16 + b as usize, true);
        println!("b = {}", b);

        while read_button(Button::Center) {}

        if b < 4 {
            task_accumulative(b);
        } else {
            task_arithmetic(b);
        }
    }
}

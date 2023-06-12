#![no_std]
#![no_main]

use core::time::Duration;

#[macro_use]
extern crate cpu_lib;

use cpu_lib::prelude::*;

fn set_tube(d: &[Option<u8>; 8]) {
    println!("{:?}", d);
    let value = d
        .iter()
        .rev()
        .fold(0u32, |v, o| (v << 4) | o.unwrap_or(0) as u32);
    let mask = d
        .iter()
        .rev()
        .fold(0u8, |v, o| (v << 1) | o.is_some() as u8);
    set_tube_value(value);
    set_tube_enable(mask);

    sleep(Duration::from_millis(200));
}

#[no_mangle]
fn main() -> i32 {
    set_tube_mode(TubeMode::Hex);

    let mut value = [None, None, None, None, None, None, None, None];

    for i in 1..=8 {
        value[8 - i] = Some(i as u8);
        set_tube(&value);
    }
    for i in 0..8 {
        value[i] = None;
        set_tube(&value);
    }

    for i in 1..=8 {
        value[i - 1] = Some(i as u8);
        set_tube(&value);
    }
    for i in 0..8 {
        value[7 - i] = None;
        set_tube(&value);
    }

    for i in 1..=8 {
        value[8 - i] = Some(i as u8);
        set_tube(&value);
        value[8 - i] = None;
    }
    for i in 0..8 {
        value[i] = Some(8 - i as u8);
        set_tube(&value);
        value[i] = None;
    }

    for i in 1..=8 {
        value[i - 1] = Some(i as u8);
        set_tube(&value);
        value[i - 1] = None;
    }
    for i in 0..8 {
        value[7 - i] = Some(8 - i as u8);
        set_tube(&value);
        value[7 - i] = None;
    }

    let templ_1 = [
        None,
        Some(1),
        Some(1),
        Some(4),
        Some(5),
        Some(1),
        Some(4),
        None,
    ];
    let templ_2 = [
        Some(1),
        Some(9),
        Some(1),
        Some(9),
        Some(8),
        Some(1),
        Some(0),
        None,
    ];
    let templ_3 = [
        Some(1),
        Some(9),
        Some(2),
        Some(6),
        Some(0),
        Some(8),
        Some(1),
        Some(7),
    ];

    for _ in 0..8 {
        templ_1
            .iter()
            .rev()
            .enumerate()
            .map(|(i, v)| if i % 2 == 0 { v } else { &None })
            .zip(value.iter_mut())
            .for_each(|(t, v)| *v = *t);
        set_tube(&value);
        templ_1
            .iter()
            .rev()
            .enumerate()
            .map(|(i, v)| if i % 2 != 0 { v } else { &None })
            .zip(value.iter_mut())
            .for_each(|(t, v)| *v = *t);
        set_tube(&value);
    }

    for _ in 0..8 {
        templ_2
            .iter()
            .rev()
            .enumerate()
            .map(|(i, v)| if i < 4 { v } else { &None })
            .zip(value.iter_mut())
            .for_each(|(t, v)| *v = *t);
        set_tube(&value);
        templ_2
            .iter()
            .rev()
            .enumerate()
            .map(|(i, v)| if i >= 4 { v } else { &None })
            .zip(value.iter_mut())
            .for_each(|(t, v)| *v = *t);
        set_tube(&value);
    }

    for _ in 0..24 {
        let idx = (random_value() % 8) as usize;
        value[idx] = templ_3[7 - idx];
        set_tube(&value);
        value.iter_mut().for_each(|v| *v = None);
        // set_tube(&value);
    }
    set_tube(&value);

    0
}

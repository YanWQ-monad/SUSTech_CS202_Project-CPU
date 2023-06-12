#![no_std]
#![no_main]

#[macro_use]
extern crate cpu_lib;

use cpu_lib::prelude::*;

#[no_mangle]
fn main() -> i32 {
    println!("Hello, world!");
    let mut buf = [0; 16];

    monitor::set_color(1);
    print!("n1: ");
    let line = read_line(&mut buf);
    let n1: i32 = line.parse().unwrap();

    monitor::set_color(2);
    print!("n2: ");
    let line = read_line(&mut buf);
    let n2: i32 = line.parse().unwrap();

    monitor::set_color(3);
    println!("n1 = {}", n1);
    monitor::set_color(4);
    println!("n2 = {}", n2);
    monitor::set_color(5);
    println!("n1 + n2 = {}", n1 + n2);

    // loop {}

    0
}

use alloc::vec::Vec;

use cpu_lib::uart;

#[derive(Debug, PartialEq, Clone, Copy)]
pub enum Input {
    Left,
    Right,
    Down,
    Rotate,
    Drop,
    Quit,
    Restart,
    Number(u32),
}

impl TryFrom<char> for Input {
    type Error = ();

    fn try_from(key: char) -> Result<Self, Self::Error> {
        match key {
            'w' => Ok(Input::Rotate),
            'd' => Ok(Input::Right),
            'a' => Ok(Input::Left),
            's' => Ok(Input::Down),
            'k' => Ok(Input::Rotate),
            'l' => Ok(Input::Right),
            'h' => Ok(Input::Left),
            'j' => Ok(Input::Down),
            ' ' => Ok(Input::Drop),
            'q' => Ok(Input::Quit),
            'r' => Ok(Input::Restart),
            c => match c {
                '0'..='9' => Ok(Input::Number(c.to_digit(10).expect("Should not fail"))),
                _ => Err(()),
            },
        }
    }
}

pub fn collect_inputs() -> Vec<Input> {
    let mut v = Vec::new();
    while uart::input_ready() {
        if let Ok(input) = Input::try_from(uart::read() as char) {
            println!("Input: {:?}", input);
            v.push(input);
        }
    }
    v
}

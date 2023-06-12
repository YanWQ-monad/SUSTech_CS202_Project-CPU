use core::cmp::{max, min};

#[derive(Debug, Clone, PartialEq)]
pub struct Level {
    pub current: u32,
    pub score: u32,
    pub cleared_lines: u32,
}

impl Level {
    pub fn new(level: u32) -> Self {
        Self {
            current: level,
            score: 0,
            cleared_lines: 0,
        }
    }

    pub const fn required_ticks(&self) -> u32 {
        match self.current {
            0 => 48,
            1 => 43,
            2 => 38,
            3 => 33,
            4 => 28,
            5 => 23,
            6 => 18,
            7 => 13,
            8 => 8,
            9 => 6,
            10..=12 => 5,
            13..=15 => 4,
            16..=18 => 3,
            19..=28 => 2,
            _ => 1,
        }
    }

    fn required_lines(&self) -> u32 {
        min(
            self.current * 10 + 10,
            max(100, (self.current * 10).saturating_sub(50)),
        )
    }

    pub fn up(&mut self, cleared: &ClearedLines) {
        self.cleared_lines += cleared.value();
        self.score += self.score(cleared);

        if self.cleared_lines >= self.required_lines() {
            self.current += 1;
        }
    }

    const fn score(&self, cleared: &ClearedLines) -> u32 {
        match cleared {
            ClearedLines::None => 0,
            ClearedLines::Single => 40 * (self.current + 1),
            ClearedLines::Double => 100 * (self.current + 1),
            ClearedLines::Triple => 300 * (self.current + 1),
            ClearedLines::Tetrs => 1200 * (self.current + 1),
        }
    }
}

impl From<usize> for ClearedLines {
    fn from(value: usize) -> Self {
        match value {
            0 => ClearedLines::None,
            1 => ClearedLines::Single,
            2 => ClearedLines::Double,
            3 => ClearedLines::Triple,
            4 => ClearedLines::Tetrs,
            _ => panic!("This must never happen"),
        }
    }
}

impl ClearedLines {
    fn value(&self) -> u32 {
        match self {
            ClearedLines::None => 0,
            ClearedLines::Single => 1,
            ClearedLines::Double => 2,
            ClearedLines::Triple => 3,
            ClearedLines::Tetrs => 4,
        }
    }
}

pub enum ClearedLines {
    None,
    Single,
    Double,
    Triple,
    Tetrs,
}

use alloc::collections::VecDeque;
use alloc::vec;

use cpu_lib::monitor::Color;

use super::{
    level::{ClearedLines, Level},
    tetromino::Tetromino,
};

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Square {
    Empty,
    Occupied(Color),
}

pub const FIELD_HEIGHT: usize = 20;
pub const FIELD_WIDTH: usize = 10;

pub type Field = VecDeque<[Square; FIELD_WIDTH]>;

#[derive(Debug, Clone, PartialEq)]
pub struct GameState {
    pub level: Level,
    pub current: Tetromino,
    pub next: Tetromino,
    pub preview: Option<Tetromino>,
    pub ticks: u32,
    pub field: Field,
}

impl GameState {
    pub fn new(level: u32) -> Self {
        let mut state = Self {
            level: Level::new(level),
            current: Tetromino::next(),
            next: Tetromino::next(),
            preview: None,
            ticks: 0,
            field: VecDeque::from(vec![[Square::Empty; 10]; 20]),
        };

        state.preview = state.determine_preview();

        state
    }
}

impl GameState {
    pub fn determine_preview(&self) -> Option<Tetromino> {
        let mut preview = self.current.clone();

        preview.move_down();

        if self.check_collision(&preview).is_some() {
            return None;
        }

        while self.check_collision(&preview).is_none() {
            preview.move_down();
        }

        preview.move_up();

        Some(preview)
    }

    pub fn try_move_down(&mut self) -> bool {
        match self.try_solidify() {
            true => true,
            false => {
                self.current.move_down();
                false
            }
        }
    }

    fn try_solidify(&mut self) -> bool {
        let mut copy = self.current.clone();
        copy.move_down();

        if self.check_collision(&copy).is_some() {
            for elem in self.current.offset_blocks().iter() {
                self.field[elem.vec.y as usize][elem.vec.x as usize] =
                    Square::Occupied(self.current.color);
            }

            return true;
        }

        false
    }

    pub fn check_collision(&self, tetromino: &Tetromino) -> Option<Collision> {
        if Self::is_out_of_bounds(tetromino) {
            return Some(Collision::OutOfBounds);
        }

        if Self::has_collision_with_block(tetromino, &self.field) {
            return Some(Collision::WithBlock);
        }

        None
    }

    fn has_collision_with_block(tetromino: &Tetromino, field: &Field) -> bool {
        tetromino.offset_blocks().iter().any(|block| {
            match &field[block.vec.y as usize][block.vec.x as usize] {
                Square::Empty => false,
                Square::Occupied(_) => true,
            }
        })
    }

    fn is_out_of_bounds(tetromino: &Tetromino) -> bool {
        tetromino.offset_blocks().iter().any(|block| {
            block.vec.x as usize >= FIELD_WIDTH
                || block.vec.x < 0
                || block.vec.y as usize >= FIELD_HEIGHT
                || block.vec.y < 0
        })
    }

    pub fn clear_lines(&mut self) -> ClearedLines {
        self.field
            .retain(|line| line.iter().any(|square| square == &Square::Empty));

        let cleared_lines = FIELD_HEIGHT - self.field.len();

        while self.field.len() < FIELD_HEIGHT {
            self.field.push_front([Square::Empty; 10]);
        }

        cleared_lines.into()
    }

    pub fn rotate(&mut self) {
        let original = self.current.clone();
        self.current.rotate();

        self.kickback(original);
    }

    pub fn move_right(&mut self) {
        let original = self.current.clone();
        self.current.move_right();

        self.kickback(original);
    }

    pub fn move_left(&mut self) {
        let original = self.current.clone();
        self.current.move_left();

        self.kickback(original);
    }

    pub fn move_down(&mut self) -> bool {
        self.try_move_down()
    }

    pub fn drop(&mut self) -> bool {
        while !self.try_move_down() {}

        true
    }

    fn kickback(&mut self, original: Tetromino) {
        if self.check_collision(&self.current).is_some() {
            self.current = original;
        }
    }

    pub fn advance_game(&mut self, already_solidified: bool) -> bool {
        self.ticks += 1;

        match self.ticks > self.level.required_ticks() {
            true => {
                self.ticks = 0;
                already_solidified || self.try_move_down()
            }
            false => already_solidified,
        }
    }

    pub fn is_finished(&self) -> bool {
        self.check_collision(&self.current).is_some()
    }
}

pub enum Collision {
    OutOfBounds,
    WithBlock,
}

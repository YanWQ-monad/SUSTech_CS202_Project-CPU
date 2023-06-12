use core::hash::{Hash, Hasher};
use rand::Rng;
// use tui::style::Color;

use cpu_lib::{monitor::Color, rng};

use super::math::{Matrix2D, Vector2D, ROTATE_CCW, ROTATE_CW};

#[derive(Debug, Clone, PartialEq, Hash)]
pub enum Rotation {
    None,
    Two(Two),
    Four(Four),
}

#[derive(Debug, Clone, PartialEq, Hash)]
pub enum Two {
    Right,
    Up,
}

#[derive(Debug, Clone, PartialEq, Hash)]
pub enum Four {
    Right,
    Up,
    Left,
    Down,
}

#[derive(Debug, Clone, PartialEq, Hash)]
pub struct Block {
    pub vec: Vector2D,
}

impl Block {
    pub const fn new(x: i32, y: i32) -> Self {
        Self {
            vec: Vector2D { x, y },
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct Tetromino {
    pub blocks: [Block; 4],
    pub rotation: Rotation,
    pub origin: Block,
    pub coords: Vector2D,
    pub color: Color,
}

impl Hash for Tetromino {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.blocks.hash(state);
        self.rotation.hash(state);
        self.coords.hash(state);
    }
}

impl Tetromino {
    pub const fn i() -> Tetromino {
        Tetromino {
            blocks: [
                Block::new(0, 1),
                Block::new(1, 1),
                Block::new(2, 1),
                Block::new(3, 1),
            ],
            rotation: Rotation::Two(Two::Right),
            origin: Block::new(2, 1),
            color: Color::Cyan,
            coords: Vector2D::default(),
        }
    }

    pub const fn o() -> Tetromino {
        Tetromino {
            blocks: [
                Block::new(1, 1),
                Block::new(2, 1),
                Block::new(1, 2),
                Block::new(2, 2),
            ],
            rotation: Rotation::None,
            origin: Block::new(2, 1),
            color: Color::Yellow,
            coords: Vector2D::default(),
        }
    }

    pub const fn t() -> Tetromino {
        Tetromino {
            blocks: [
                Block::new(0, 1),
                Block::new(1, 1),
                Block::new(2, 1),
                Block::new(1, 2),
            ],
            rotation: Rotation::Four(Four::Right),
            origin: Block::new(1, 1),
            color: Color::Magenta,
            coords: Vector2D::default(),
        }
    }

    pub const fn s() -> Tetromino {
        Tetromino {
            blocks: [
                Block::new(1, 1),
                Block::new(2, 1),
                Block::new(0, 2),
                Block::new(1, 2),
            ],
            rotation: Rotation::Two(Two::Right),
            origin: Block::new(1, 1),
            color: Color::Green,
            coords: Vector2D::default(),
        }
    }

    pub const fn z() -> Tetromino {
        Tetromino {
            blocks: [
                Block::new(0, 1),
                Block::new(1, 1),
                Block::new(1, 2),
                Block::new(2, 2),
            ],
            rotation: Rotation::Two(Two::Right),
            origin: Block::new(1, 1),
            color: Color::Red,
            coords: Vector2D::default(),
        }
    }

    pub const fn j() -> Tetromino {
        Tetromino {
            blocks: [
                Block::new(0, 1),
                Block::new(1, 1),
                Block::new(2, 1),
                Block::new(2, 2),
            ],
            rotation: Rotation::Four(Four::Right),
            origin: Block::new(1, 1),
            color: Color::Blue,
            coords: Vector2D::default(),
        }
    }

    pub const fn l() -> Tetromino {
        Tetromino {
            blocks: [
                Block::new(0, 1),
                Block::new(1, 1),
                Block::new(2, 1),
                Block::new(0, 2),
            ],
            rotation: Rotation::Four(Four::Right),
            origin: Block::new(1, 1),
            color: Color::White,
            coords: Vector2D::default(),
        }
    }

    pub fn next() -> Tetromino {
        match rng::rng().gen_range(0..7) {
            0 => Tetromino::i(),
            1 => Tetromino::o(),
            2 => Tetromino::t(),
            3 => Tetromino::s(),
            4 => Tetromino::z(),
            5 => Tetromino::j(),
            6 => Tetromino::l(),
            _ => panic!("Cannot trust that crate, huh?"),
        }
    }

    pub fn move_right(&mut self) {
        self.coords.x += 1;
    }

    pub fn move_left(&mut self) {
        self.coords.x -= 1;
    }

    pub fn move_down(&mut self) {
        self.coords.y += 1;
    }

    pub fn move_up(&mut self) {
        self.coords.y -= 1;
    }

    pub fn rotate(&mut self) {
        match &self.rotation {
            Rotation::None => (),
            Rotation::Two(two) => match two {
                Two::Right => {
                    self.rotate_by(&ROTATE_CCW);
                    self.rotation = Rotation::Two(Two::Up);
                }
                Two::Up => {
                    self.rotate_by(&ROTATE_CW);
                    self.rotation = Rotation::Two(Two::Right);
                }
            },
            Rotation::Four(four) => {
                self.rotation = Rotation::Four(match four {
                    Four::Down => Four::Right,
                    Four::Right => Four::Up,
                    Four::Up => Four::Left,
                    Four::Left => Four::Down,
                });
                self.rotate_by(&ROTATE_CCW);
            }
        }
    }

    fn rotate_by(&mut self, rotation: &Matrix2D) {
        for elem in self.blocks.iter_mut() {
            elem.vec = elem.vec.rotate(&self.origin.vec, rotation);
        }
    }

    pub const fn offset_blocks(&self) -> [Block; 4] {
        [
            self.offset_block(0),
            self.offset_block(1),
            self.offset_block(2),
            self.offset_block(3),
        ]
    }

    const fn offset_block(&self, index: usize) -> Block {
        Block {
            vec: Vector2D {
                x: self.blocks[index].vec.x + self.coords.x,
                y: self.blocks[index].vec.y + self.coords.y,
            },
        }
    }
}

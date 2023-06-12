use alloc::borrow::Cow;
use alloc::vec::Vec;
use alloc::{format, vec};

use cpu_lib::monitor::monitor;
use cpu_lib::monitor::{Color, SCREEN_HEIGHT, SCREEN_WIDTH};

use crate::game::{
    level::Level,
    phase::Phase,
    state::{Field, GameState, Square, FIELD_HEIGHT, FIELD_WIDTH},
    tetromino::Tetromino,
};

const LEVEL_WIDTH: usize = 17;
const GAME_WIDTH: usize = FIELD_WIDTH * 2 + 2;

const WINDOW_HEIGHT: usize = FIELD_HEIGHT + 2;
const WINDOW_WIDTH: usize = LEVEL_WIDTH + GAME_WIDTH;

const WINDOW_X: usize = ((SCREEN_WIDTH - WINDOW_WIDTH) / 2).saturating_sub(1);
const WINDOW_Y: usize = ((SCREEN_HEIGHT - WINDOW_HEIGHT) / 2).saturating_sub(1);

#[derive(Default)]
pub struct Ui;

#[derive(Debug)]
pub struct Rect {
    pub x: usize,
    pub y: usize,
    pub width: usize,
    pub height: usize,
}

#[derive(Debug, Clone, Copy)]
pub struct ColorPair {
    pub fg: Color,
    pub bg: Color,
}

#[derive(Debug)]
pub struct Span {
    pub content: Cow<'static, str>,
    pub style: ColorPair,
}

impl Rect {
    pub fn new(x: usize, y: usize, width: usize, height: usize) -> Rect {
        Rect {
            x,
            y,
            width,
            height,
        }
    }

    fn draw_border(&self, title: &str) {
        monitor::set_color(ColorPair::default().as_u8());

        monitor::set_xy(self.x, self.y); // top
        for _ in 0..self.width {
            monitor::putchar(0xA1);
        }
        monitor::set_xy(self.x, self.y + self.height - 1); // bottom
        for _ in 0..self.width {
            monitor::putchar(0xA1);
        }
        for y in 1..self.height {
            monitor::set_xy(self.x, y + self.y);
            monitor::putchar(0xA0);
            monitor::set_xy(self.x + self.width - 1, y + self.y);
            monitor::putchar(0xA0);
        }

        monitor::set_xy(self.x, self.y);
        monitor::putchar(0xA2);
        monitor::set_xy(self.x + self.width - 1, self.y);
        monitor::putchar(0xA3);
        monitor::set_xy(self.x, self.y + self.height - 1);
        monitor::putchar(0xA4);
        monitor::set_xy(self.x + self.width - 1, self.y + self.height - 1);
        monitor::putchar(0xA5);

        let offset_x = (self.width - title.len() - 2) / 2;
        monitor::set_xy(self.x + offset_x, self.y);
        monitor::putchar(b' ');
        title.as_bytes().iter().for_each(|c| monitor::putchar(*c));
        monitor::putchar(b' ');
    }

    fn draw_content(&self, rows: &[Line]) {
        for (y, row) in rows.iter().enumerate() {
            let sy = self.y + 1 + y;
            monitor::set_xy(self.x + 1, sy);
            for cell in &row.cells {
                monitor::set_color(cell.style.as_u8());
                for c in cell.str.as_bytes() {
                    monitor::putchar(*c);
                }
            }
        }
    }

    fn draw_spans(&self, rows: &[Span]) {
        // assert!(N == self.height - 2);

        for (y, row) in rows.iter().enumerate() {
            let sy = self.y + 1 + y;
            monitor::set_xy(self.x + 1, sy);
            monitor::set_color(row.style.as_u8());
            for c in row.content.as_ref().as_bytes() {
                monitor::putchar(*c);
            }
        }
    }
}

impl ColorPair {
    fn new(fg: Color, bg: Color) -> ColorPair {
        ColorPair { fg, bg }
    }

    fn bg(&self, bg: Color) -> ColorPair {
        ColorPair { fg: self.fg, bg }
    }

    fn as_u8(&self) -> u8 {
        (self.bg as u8) << 4 | (self.fg as u8)
    }
}

impl Default for ColorPair {
    fn default() -> Self {
        ColorPair {
            fg: Color::default_fg(),
            bg: Color::default_bg(),
        }
    }
}

impl Span {
    fn new(content: &'static str, style: ColorPair) -> Span {
        Span { content: Cow::from(content), style }
    }

    fn default_style(content: Cow<'static, str>) -> Span {
        Span {
            content,
            style: ColorPair::default(),
        }
    }
}

fn game_area() -> Rect {
    Rect::new(WINDOW_X, WINDOW_Y, GAME_WIDTH, WINDOW_HEIGHT)
}

fn next_area() -> Rect {
    Rect::new(WINDOW_X + GAME_WIDTH, WINDOW_Y, GAME_WIDTH, 8)
}

fn stats_area() -> Rect {
    Rect::new(
        WINDOW_X + GAME_WIDTH,
        WINDOW_Y + 8,
        GAME_WIDTH,
        WINDOW_HEIGHT - 8,
    )
}

impl Ui {
    pub fn draw(&mut self, phase: &Phase) {
        draw_frame(phase);
    }

    pub fn draw_border(&mut self) {
        draw_border();
    }
}

fn draw_frame(phase: &Phase) {
    match phase {
        Phase::Running(running) => draw_tetrs(&running.state),
        Phase::Finished(finished) => draw_tetrs(&finished.state),
    };
}

fn draw_border() {
    game_area().draw_border("tetris");
    next_area().draw_border("next");
    stats_area().draw_border("stats");
}

fn draw_tetrs(state: &GameState) {
    let stats_area = stats_area();
    let game_area = game_area();
    let next_area = next_area();

    let line_vec: Vec<Line> = core::iter::repeat(Line::default())
        .take(FIELD_HEIGHT)
        .collect();
    let mut lines: [Line; FIELD_HEIGHT] = line_vec.try_into().unwrap();

    let next_lines_vec: Vec<Line> = core::iter::repeat(Line::default()).take(6).collect();
    let mut next_lines: [Line; 6] = next_lines_vec.try_into().unwrap();

    draw_field(state, &mut lines);
    draw_next(&state.next, &mut next_lines);
    game_area.draw_content(&lines);
    next_area.draw_content(&next_lines);

    let stats = draw_stats(&state.level);
    stats_area.draw_spans(&stats);
}

fn draw_field<'a>(state: &GameState, rows: &'a mut [Line; FIELD_HEIGHT]) {
    // if let Some(preview) = &state.preview {
    //     draw_tetromino(preview, rows, Cell::preview(preview));
    // }
    draw_tetromino(&state.current, rows, Cell::normal(&state.current));
    draw_solidified(&state.field, rows);
}

fn draw_tetromino(tetromino: &Tetromino, rows: &mut [Line], cell: Cell) {
    for elem in tetromino.blocks.iter() {
        rows[(tetromino.coords.y + elem.vec.y) as usize].cells
            [(tetromino.coords.x + elem.vec.x) as usize] = cell.clone();
    }
}

fn draw_solidified(field: &Field, rows: &mut [Line; FIELD_HEIGHT]) {
    for (line_index, line) in field.iter().enumerate() {
        for (column_index, square) in line.iter().enumerate() {
            if let Square::Occupied(color) = square {
                rows[line_index].cells[column_index] = Cell {
                    str: "  ",
                    style: ColorPair::default().bg(*color),
                };
            }
        }
    }
}

fn draw_next<'a>(next: &Tetromino, rows: &'a mut [Line; 6]) {
    draw_tetromino(next, rows, Cell::normal(next));
}

fn draw_stats(level: &Level) -> Vec<Span> {
    vec![
        Span::default_style("".into()),
        Span::default_style(format!("   Level: {}", level.current).into()),
        Span::default_style(format!("   Lines: {}", level.cleared_lines).into()),
        Span::default_style(format!("   Score: {}", level.score).into()),
    ]
}

#[derive(Clone, Debug)]
struct Line {
    pub cells: [Cell; FIELD_WIDTH],
}

impl Default for Line {
    fn default() -> Self {
        let cells: Vec<Cell> = core::iter::repeat(Cell::default())
            .take(FIELD_WIDTH)
            .collect();

        Self {
            cells: cells.try_into().unwrap(),
        }
    }
}

#[derive(Debug, Clone)]
struct Cell {
    pub str: &'static str,
    pub style: ColorPair,
}

impl Default for Cell {
    fn default() -> Self {
        Self {
            str: "  ",
            style: Default::default(),
        }
    }
}

impl Cell {
    fn normal(tetromino: &Tetromino) -> Self {
        Self {
            str: "  ",
            style: ColorPair::default().bg(tetromino.color),
        }
    }

    // fn preview(tetromino: &Tetromino) -> Self {
    //     Self {
    //         str: "◤◢",
    //         style: ColorPair::default().fg(tetromino.color),
    //     }
    // }
}

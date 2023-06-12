use alloc::boxed::Box;
use core::mem::swap;

use crate::{
    game::{state::GameState, tetromino::Tetromino},
    input::Input,
};

use super::finished::Finished;

#[derive(Debug, Clone, PartialEq)]
pub struct Running {
    pub state: GameState,
}

impl Running {
    pub fn handle(&mut self, inputs: &[Input]) -> Option<Box<Finished>> {
        if self.state.is_finished() {
            return Some(Box::new(Finished {
                state: self.state.clone(),
            }));
        }

        let solidified = self.handle_inputs(inputs);
        let solidified = self.state.advance_game(solidified);

        if solidified {
            let cleared_lines = self.state.clear_lines();
            self.state.level.up(&cleared_lines);

            swap(&mut self.state.current, &mut self.state.next);
            self.state.next = Tetromino::next();
        }

        self.state.preview = self.state.determine_preview();

        None
    }

    fn handle_inputs(&mut self, inputs: &[Input]) -> bool {
        let input = inputs.iter().next();

        match input {
            Some(i) => self.handle_input(i),
            None => false,
        }
    }

    fn handle_input(&mut self, input: &Input) -> bool {
        match input {
            Input::Right => self.state.move_right(),
            Input::Left => self.state.move_left(),
            Input::Rotate => self.state.rotate(),
            _ => (),
        }

        match input {
            Input::Down => self.state.move_down(),
            Input::Drop => self.state.drop(),
            _ => false,
        }
    }
}

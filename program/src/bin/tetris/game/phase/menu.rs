use crate::{game::state::GameState, input::Input};

use super::running::Running;

#[derive(Debug, Clone, PartialEq)]
pub struct Menu {}

impl Menu {
    pub fn handle(&self, inputs: &[Input]) -> Option<Box<Running>> {
        let input = inputs.iter().find_map(|&x| match x {
            Input::Number(number) => Some(number),
            _ => None,
        });

        input.map(|level| {
            Box::new(Running {
                state: GameState::new(level),
            })
        })
    }
}

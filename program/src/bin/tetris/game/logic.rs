use alloc::boxed::Box;
use alloc::vec::Vec;

use crate::input::{collect_inputs, Input};

use super::{phase::running::Running, phase::Phase};
use crate::game::state::GameState;

#[derive(PartialEq)]
pub enum End {
    Quit,
    Restart,
}

pub enum TickResult {
    Phase(Phase),
    End(End),
}

impl From<Input> for Option<End> {
    fn from(value: Input) -> Self {
        match value {
            Input::Restart => Some(End::Restart),
            Input::Quit => Some(End::Quit),
            _ => None,
        }
    }
}

pub struct Logic {
    phase: Phase,
}

impl Logic {
    pub fn new() -> Self {
        Self {
            phase: Phase::Running(Box::new(Running {
                state: GameState::new(1),
            })),
        }
    }

    pub fn update(&mut self) -> TickResult {
        let inputs: Vec<Input> = collect_inputs();

        if let Some(end) = self.check_for_end(&inputs) {
            return TickResult::End(end);
        }

        match &mut self.phase {
            Phase::Running(running) => {
                if let Some(finished) = running.handle(&inputs) {
                    self.phase = Phase::Finished(finished);
                }
            }
            Phase::Finished(finished) => finished.handle(),
        };

        TickResult::Phase(self.phase.clone())
    }

    fn check_for_end(&self, inputs: &[Input]) -> Option<End> {
        inputs.iter().find_map(|&x| x.into())
    }
}

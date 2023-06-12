use crate::game::state::GameState;

#[derive(Debug, Clone, PartialEq)]
pub struct Finished {
    pub state: GameState,
}

impl Finished {
    pub fn handle(&self) {}
}

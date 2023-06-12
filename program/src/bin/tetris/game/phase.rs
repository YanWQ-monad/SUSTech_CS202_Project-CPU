use alloc::boxed::Box;

use self::{finished::Finished, running::Running};

pub mod finished;
// pub mod menu;
pub mod running;

#[derive(Debug, Clone, PartialEq)]
pub enum Phase {
    Running(Box<Running>),
    Finished(Box<Finished>),
}

use core::time::Duration;

use cpu_lib::time::{sleep, Instant};

use crate::ui::Ui;

use super::logic::{End, Logic, TickResult};

const TICKS_PER_SECOND: f64 = 60f64;

pub struct GameLoop {
    tick_duration: Duration,
    previous: Instant,
    current: Instant,
    accumulated: Duration,
    logic: Logic,
    ui: Ui,
}

impl GameLoop {
    pub fn new(logic: Logic, ui: Ui) -> Self {
        let instant = Instant::now();

        GameLoop {
            tick_duration: Duration::from_secs_f64(1.0 / TICKS_PER_SECOND),
            previous: instant,
            current: instant,
            accumulated: Duration::default(),
            logic,
            ui,
        }
    }

    pub fn run(&mut self) -> End {
        self.ui.draw_border();
        loop {
            if let Some(end) = self.iterate() {
                return end;
            }
        }
    }

    fn iterate(&mut self) -> Option<End> {
        self.current = Instant::now();

        let mut elapsed = self.current - self.previous;
        self.previous = self.current;

        if elapsed > self.tick_duration {
            elapsed = self.tick_duration;
        }

        self.accumulated += elapsed;

        if self.accumulated >= self.tick_duration {
            self.accumulated -= self.tick_duration;

            match self.logic.update() {
                TickResult::End(end) => {
                    return Some(end);
                }
                TickResult::Phase(phase) => self.ui.draw(&phase),
            };
        } else {
            self.idle(self.tick_duration - self.accumulated);
        }

        None
    }

    fn idle(&self, difference: Duration) {
        sleep(difference);
    }
}

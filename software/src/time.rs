use core::ops::Sub;
use core::time::Duration;

use crate::board::read_u32;

pub const CPU_FREQUENCY: usize = 20_000_000;

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub struct Instant(Duration);

impl Instant {
    pub fn now() -> Instant {
        Instant(Duration::from_micros((read_cycles() / 10) as u64))
    }

    pub fn duration_since(&self, earlier: Instant) -> Duration {
        self.0 - earlier.0
    }
}

impl Sub<Instant> for Instant {
    type Output = Duration;
    fn sub(self, other: Instant) -> Duration {
        self.duration_since(other)
    }
}

pub fn read_cycles() -> usize {
    unsafe { read_u32(0xFFFFF010) as usize }
}

pub fn sleep(duration: Duration) {
    let until = Instant::now().0 + duration;
    while Instant::now().0 < until {}
}

use rand::{Error, RngCore};

use crate::board::random_value;

pub struct FpgaRng;

pub fn rng() -> FpgaRng {
    FpgaRng
}

impl RngCore for FpgaRng {
    #[inline]
    fn next_u32(&mut self) -> u32 {
        random_value()
    }

    fn next_u64(&mut self) -> u64 {
        let v1 = self.next_u32();
        let v2 = self.next_u32() ^ self.next_u32();
        (v1 as u64) << 32 | (v2 as u64)
    }

    fn fill_bytes(&mut self, dest: &mut [u8]) {
        dest.iter_mut().for_each(|ptr| {
            let v = self.next_u32();
            let p1 = (v >> 24) & 0x3;
            let p2 = (v >> 16) & 0x3;
            let p3 = (v >> 8) & 0x3;
            let p4 = (v >> 0) & 0x3;
            let v = (p1 << 6) | (p2 << 4) | (p3 << 2) | p4;
            *ptr = v as u8;
        });
    }

    fn try_fill_bytes(&mut self, dest: &mut [u8]) -> Result<(), Error> {
        self.fill_bytes(dest);
        Ok(())
    }
}

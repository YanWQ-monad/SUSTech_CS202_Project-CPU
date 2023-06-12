pub struct Matrix2D {
    pub x0: i32,
    pub x1: i32,
    pub y0: i32,
    pub y1: i32,
}

pub const ROTATE_CCW: Matrix2D = Matrix2D {
    x0: 0,
    x1: 1,
    y0: -1,
    y1: 0,
};

pub const ROTATE_CW: Matrix2D = Matrix2D {
    x0: 0,
    x1: -1,
    y0: 1,
    y1: 0,
};

#[derive(Debug, Clone, PartialEq, Hash)]
pub struct Vector2D {
    pub x: i32,
    pub y: i32,
}

impl Vector2D {
    // This is explicitly not implemented as the Default trait in order to make it const
    pub const fn default() -> Self {
        Self { x: 3, y: 0 }
    }

    pub fn rotate(&self, center: &Vector2D, rot: &Matrix2D) -> Vector2D {
        Vector2D {
            x: rot.x0 * self.x + rot.x1 * self.y + center.x - rot.x0 * center.x - rot.x1 * center.y,
            y: rot.y0 * self.x + rot.y1 * self.y + center.y - rot.y0 * center.x - rot.y1 * center.y,
        }
    }
}

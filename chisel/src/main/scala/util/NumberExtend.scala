package util

import chisel3._
import chisel3.util._

object SignExt {
  def apply(n: Int, x: UInt): UInt = {
    val m = n - x.getWidth
    val highest = x(x.getWidth - 1)
    val signs = Fill(m, highest)
    Cat(signs, x)
  }
}

object SignExt32 {
  def apply(x: UInt): UInt = SignExt(32, x)
}

object ZeroExt {
  def apply(n: Int, x: UInt): UInt = {
    val m = n - x.getWidth
    val signs = Fill(m, 0.B)
    Cat(signs, x)
  }
}

object ZeroExt32 {
  def apply(x: UInt): UInt = ZeroExt(32, x)
}

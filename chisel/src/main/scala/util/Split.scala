package util

import chisel3._

object Split {
  def apply(x: UInt, n: Int, len: Int): Vec[UInt] = {
    val vec = Wire(Vec(n, UInt(len.W)))
    for (i <- 0 until n)
      vec(i) := x.asUInt((i + 1) * len - 1, i * len)
    vec
  }
}

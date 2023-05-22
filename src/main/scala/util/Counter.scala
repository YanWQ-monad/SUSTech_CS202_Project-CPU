package util

import chisel3._

object Counter {
  def maxBits(bits: Int): UInt = {
    val x = RegInit(0.U(bits.W))
    x := x + 1.U
    x
  }

  def maxBits(bits: Int, en: Bool): UInt = {
    val x = RegInit(0.U(bits.W))
    x := Mux(en, x + 1.U, x)
    x
  }
}

object Pulse {
  def apply(n: UInt): Bool = n.andR
}

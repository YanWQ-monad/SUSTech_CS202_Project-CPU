package util

import chisel3._
import chisel3.util._

object Encoder {
  def apply(width: Int)(in: UInt): UInt = {
    val inWidth = Helper.log2(width)
    require(inWidth == in.getWidth)

    val table = LazyList.range(0, width)
      .map(b => b.U(inWidth.W) -> (BigInt(1) << b).U(width.W))

    MuxLookup(in, 0.U)(table)
  }
}

object Decoder {
  def apply(width: Int)(in: UInt): UInt = {
    val outWidth = Helper.log2(width)

    val table = LazyList.range(0, width)
      .map(b => (BigInt(1) << b).U(width.W) -> b.U(outWidth.W))

    MuxLookup(in, 0.U)(table)
  }
}

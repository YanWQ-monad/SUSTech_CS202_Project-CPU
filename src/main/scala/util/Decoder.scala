package util

import chisel3._
import chisel3.util._

object Decoder {
  def apply(width: Int)(in: UInt): UInt = {
    val outWidth = Helper.log2(width)

    val table = LazyList.range(0, width - 1)
      .map(b => (BigInt(1) << b).U(width.W) -> b.U(outWidth.W))

    MuxLookup(in, 0.U)(table)
  }
}

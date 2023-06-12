package core

import misc._
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TubesSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "misc.Tubes"

  def asUnsigned(x: Int) = (BigInt(x >>> 1) << 1) + (x & 1)

  it should "read and write word" in {
    test(new DivTen()) { implicit dut =>
      val rand = new scala.util.Random
      for (_ <- 0 until 1024) {
        val v = asUnsigned(rand.nextInt())
//        println(v)
        dut.io.in.poke(v)

        dut.io.quotient.expect(v / 10)
        dut.io.remainder.expect(v % 10)
      }
    }
  }
}

package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.util.experimental.loadMemoryFromFile

class CoreSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "core.Core"

  val regNames = Array("x0", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6")

  it should "work" in {
    test(new Core()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (_ <- 0 until 16) {
        val pc = dut.io.pc.peek().litValue
        println(f"${pc}%x " + dut.io.invalid.peek())
        dut.clock.step(1)

        for (i <- 0 until 32) {
          dut.io.debugRegAddr.poke(i.U)
          val value = dut.io.debugRegData.peek().litValue
          val name = regNames(i)
          print(f"${name}%3s: ${value}%08x  ")
          if ((i + 1) % 8 == 0)
            println()
        }
      }
    }
  }
}

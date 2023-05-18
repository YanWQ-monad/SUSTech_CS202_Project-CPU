package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.control._

class CoreSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "core.Core"

  implicit val debugMode = true

  val regNames = Array("x0", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6")

  def asUnsigned(x: Int) = (BigInt(x >>> 1) << 1) + (x & 1)

//  it should "work" in {
//    test(new Core()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
//      dut.clock.step(1)
//
//      val loop = new Breaks
//      loop.breakable {
//        for (i <- 0 until 512) {
//          val pc = dut.debug.get.pc.peek().litValue
//          println(f"PC = 0x${pc}%x")
////          println(f"${i}%d, PC: 0x${pc}%x  invalid flag: ${dut.debug.get.invalid.peek().litValue}")
//          dut.clock.step(2)
//
//          for (i <- 0 until 32) {
//            dut.debug.get.debugRegAddr.poke(i.U)
//            val value = asUnsigned(dut.debug.get.debugRegData.peek().litValue.toInt)
//            val name = regNames(i)
//            print(f"${name}%3s: ${value}%08x  ")
//            if ((i + 1) % 8 == 0)
//              println()
//          }
//
//          val value = dut.io.extOut.peek().litValue
//          println(f"val: ${value}%08x")
//
//          if (pc == 0xd0 || (pc & 0x3) > 0)
//            loop.break
//
//          //        for  (i <- 0 until 8) {
//          //          val value = dut.io.externalOut(i).peek().litValue
//          //          print(f"${i}%d: ${value}%08x  ")
//          //        }
//          //        println()
//          println()
//        }
//      }
//    }
//  }
}

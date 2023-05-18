package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TestModule extends Module {
  val io = IO(new Bundle {
    val extIn = Input(UInt(8.W))
    val extOut = Output(UInt(32.W))

    val uartTx = Output(Bool())
    val uartRx = Input(Bool())
  })

  val debug = IO(new Bundle {
    val pc = Output(UInt(32.W))
    val invalid = Output(Bool())
    val debugRegAddr = Input(UInt(5.W))
    val debugRegData = Output(SInt(32.W))
  })

//  implicit val debugMode = true
//  val dut = Module(new Core())
//
//  dut.clocks.uart := clock
//
//  io <> dut.io
//  debug <> dut.debug.get
}

class UARTSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "UART"

  val regNames = Array("x0", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6")

  def asUnsigned(x: Int) = (BigInt(x >>> 1) << 1) + (x & 1)

  it should "send and receive data via UART" in {
    test(new TestModule()).withAnnotations(Seq(WriteVcdAnnotation)) { implicit dut =>
      fork {
        dut.clock.step(1)

        for (i <- 0 until 256) {
          val pc = dut.debug.pc.peek().litValue
          println(f"PC = 0x${pc}%x")
          //          println(f"${i}%d, PC: 0x${pc}%x  invalid flag: ${dut.debug.invalid.peek().litValue}")
          dut.clock.step(2)

          for (i <- 0 until 32) {
            dut.debug.debugRegAddr.poke(i.U)
            val value = asUnsigned(dut.debug.debugRegData.peek().litValue.toInt)
            val name = regNames(i)
            print(f"${name}%3s: ${value}%08x  ")
            if ((i + 1) % 8 == 0)
              println()
          }

          val value = dut.io.extOut.peek().litValue
          println(f"val: ${value}%08x")

          //        if (pc == 0xd0 || (pc & 0x3) > 0)
          //          loop.break

          //        for  (i <- 0 until 8) {
          //          val value = dut.io.externalOut(i).peek().litValue
          //          print(f"${i}%d: ${value}%08x  ")
          //        }
          //        println()
          println()
        }
      } .fork {
        def sendSignal(s: Boolean) = for (_ <- 0 until 8) {
          dut.io.uartRx.poke(s.B)
          dut.clock.step()
        }
        dut.io.uartRx.poke(true.B)
        dut.clock.step(12)

        val bits = Array(0, 1, 1, 0, 1, 0, 1, 1)

        sendSignal(false)  // start
        for (bit <- bits) {
          sendSignal(bit > 0)
        } // bit
        sendSignal(true)  // stop1
        sendSignal(true)  // stop2

        val bits2 = Array(1, 0, 0, 1, 1, 1, 0, 1)
        sendSignal(false)  // start
        for (bit <- bits2) {
          sendSignal(bit > 0)
        } // bit
        sendSignal(true)  // stop1
        sendSignal(true)  // stop2
        for (_ <- 0 until 512 / 8)
          sendSignal(true)
      } .fork {
        for (_ <- 0 until 732) {
          dut.clock.step()
          val value = dut.io.uartTx.peek().litValue
          println(f"==> UART val: ${value}%d")
        }
      } .join()
    }
  }
}

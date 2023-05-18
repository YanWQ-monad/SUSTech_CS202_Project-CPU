package ip

import util.GenerateOptions
import chisel3._

private class RawClockWizard extends BlackBox {
  val io = IO(new Bundle {
    val reset = Input(Reset())
    val clk_in1 = Input(Clock())    // 100   MHz
    val clk_std = Output(Clock())   //  10   MHz
    val clk_vga = Output(Clock())   //  40   MHz
    val clk_uart = Output(Clock())  //   8.5 MHz
    val locked = Output(Bool())
  })

  override def desiredName = "clk_wiz_0"
}

class ClockWizard(implicit options: GenerateOptions) extends Module {
  val io = IO(new Bundle {
    val clockStd = Output(Clock())
    val clockVga = Output(Clock())
    val clockUart = Output(Clock())
    val reset = Output(Reset())
  })

  if (options.useIP) {
    val raw = Module(new RawClockWizard())

    // todo: can't init "cnt"
    withReset (0.B.asAsyncReset) {  // disable reset to clock
      withClock(raw.io.clk_uart) {
        val cnt = RegInit(0.U(11.W)) // 8x
        cnt := cnt + 111.U
        io.clockUart := cnt(10).asClock
      }
    }

    raw.io.reset := false.B.asAsyncReset
    raw.io.clk_in1 := clock
    io.clockStd := raw.io.clk_std
    io.clockVga := raw.io.clk_vga
    io.reset := (reset.asBool || !raw.io.locked).asAsyncReset
  }
  else {
    io.clockStd := clock

    withReset (0.B.asAsyncReset) {
      val stdCnt = RegInit(0.U(2.W))
      stdCnt := stdCnt + 1.U
      io.clockUart := stdCnt(1).asClock
    }

    io.clockVga := clock   // ignore now
    io.reset := reset
  }
}

package ip

import util.GenerateOptions
import chisel3._

private class RawClockWizard extends BlackBox {
  val io = IO(new Bundle {
    val reset = Input(Reset())
    val clk_in1 = Input(Clock())    // 100   MHz
    val clk_std = Output(Clock())   //  40   MHz
    val clk_vga = Output(Clock())   //  40   MHz
    val clk_uart = Output(Clock())  //  12.5 MHz
    val locked = Output(Bool())
  })

  override def desiredName = "clk_wiz_0"
}

class ClockWizard(implicit options: GenerateOptions) extends Module {
  val io = IO(new Bundle {
    val clockCpu = Output(Clock())
    val clockVga = Output(Clock())
    val clockUart = Output(Clock())
    val reset = Output(Reset())
  })

  if (options.useIP) {
    val raw = Module(new RawClockWizard())

    // UART needs to slow down further
    // 12.5 MHz * (151 / 1024) = 1.8432617188 MHz ~ 115203.857425 Hz * 16(oversample)
    withReset (0.B.asAsyncReset) {  // disable reset to clock
      withClock(raw.io.clk_uart) {
        val cnt = RegInit(0.U(10.W))
        cnt := cnt + 151.U
        io.clockUart := cnt(cnt.getWidth - 1).asClock
      }
    }

    raw.io.reset := false.B.asAsyncReset
    raw.io.clk_in1 := clock
    io.clockCpu := raw.io.clk_std
    io.clockVga := raw.io.clk_vga

    val syncReset = Wire(Reset())
    syncReset := (reset.asBool || !raw.io.locked)
    io.reset := syncReset
  }
  else {
    // at simulation, the clock doesn't need to be cared a lot
    io.clockCpu := clock

    withReset (0.B.asAsyncReset) {
      val stdCnt = RegInit(0.U(2.W))
      stdCnt := stdCnt + 1.U
      io.clockUart := stdCnt(1).asClock
    }

    io.clockVga := clock   // ignore now
    io.reset := reset
  }
}

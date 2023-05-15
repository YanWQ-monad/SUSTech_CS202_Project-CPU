package ip

import chisel3._

private class RawClockWizard extends BlackBox {
  val io = IO(new Bundle {
    val reset = Input(Reset())
    val clk_in1 = Input(Clock())
    val clk_out1 = Output(Clock())
    val locked = Output(Bool())
  })

  override def desiredName = "clk_wiz_0"
}

class ClockWizard(implicit debugMode: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val clockOut = Output(Clock())
    val reset = Output(Reset())
  })

  if (debugMode) {
    io.clockOut := clock
    io.reset := false.B.asAsyncReset
  }
  else {
    val raw = Module(new RawClockWizard())

    raw.io.reset := reset
    raw.io.clk_in1 := clock
    io.clockOut := raw.io.clk_out1
    io.reset := (!raw.io.locked).asAsyncReset
  }
}

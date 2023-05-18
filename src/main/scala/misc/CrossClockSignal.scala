package misc

import chisel3._
import chisel3.util._

class CrossClockSignal extends Module {
  val io = IO(new Bundle {
    val clockSrc = Input(Clock())
    val clockDst = Input(Clock())
    val signalSrc = Input(Bool())
    val signalDst = Output(Bool())
  })

  val toggleSrc = withClock(io.clockSrc) {
    val toggleSrc = RegInit(0.B)
    toggleSrc := toggleSrc ^ io.signalSrc
    toggleSrc
  }

  val shiftDst = withClock(io.clockDst) {
    ShiftRegisters(toggleSrc, 4)
  }

  io.signalDst := shiftDst(3) ^ shiftDst(2)
}

object SignalFromClock {
  def apply(signal: Bool, clock: Clock): Bool = {
    val xdd = Module(new CrossClockSignal())
    xdd.io.clockSrc := clock
    xdd.io.signalSrc := signal
    xdd.io.signalDst
  }
}

object SignalToClock {
  def apply(clockSrc: Clock, signal: Bool, clockDst: Clock): Bool = withClock(clockDst) {
    SignalFromClock(signal, clockSrc)
  }
}

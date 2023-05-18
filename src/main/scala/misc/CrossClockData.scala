package misc

import chisel3._

class CrossClockData[T <: Data](gen: => T) extends Module {
  val io = IO(new Bundle {
    val clockSrc = Input(Clock())
    val dataSrc = Input(gen)
    val dataDst = Output(gen)
  })

  val mem = Mem(1, gen)
  io.dataDst := mem.read(0.U)
  mem.write(0.U, io.dataSrc, io.clockSrc)
}

object DataFromClock {
  def apply[T <: Data](gen: => T, data: T, clock: Clock): T = {
    val xdd = Module(new CrossClockData(gen))
    xdd.io.clockSrc := clock
    xdd.io.dataSrc := data
    xdd.io.dataDst
  }
}

object DataToClock {
  def apply[T <: Data](gen: => T, clockSrc: Clock, data: T, clockDst: Clock): T = withClock(clockDst) {
    DataFromClock(gen, data, clockSrc)
  }
}

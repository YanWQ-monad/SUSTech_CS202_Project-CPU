import core._
import util._
import board._
import ip._
import chisel3._

class Top(implicit options: GenerateOptions) extends Module {
  val out = IO(new RealBoardDataBundle)

  val clocks = Module(new ClockWizard())
  val board = withClockAndReset(clocks.io.clockCpu, clocks.io.reset) { Module(new BoardDataController()) }
  val core = withClockAndReset(clocks.io.clockCpu, clocks.io.reset) { Module(new Core()) }

  board.inside <> core.io.external
  board.outer <> out
  board.io.uartClock := clocks.io.clockUart
  board.io.vgaClock := clocks.io.clockVga
  board.io.cpuClock := clocks.io.clockCpu
  board.io.cycles := core.io.cycles
  board.io.originPC := core.io.originPC
  core.io.interrupt := board.io.interrupt
}

object VerilogMain extends App {
  implicit val options = new GenerateOptions(
    false,
    true,
    100_000_000,
    20_000_000,
    12_500_000,
    40_000_000)

  Emit(new Top(), args)
}

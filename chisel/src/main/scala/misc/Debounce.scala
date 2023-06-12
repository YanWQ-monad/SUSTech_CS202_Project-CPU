package misc

import util.{GenerateOptions, Helper}
import chisel3._
import chisel3.util._

class DebounceOutBundle extends Bundle {
  val out = Output(Bool())
  val onDown = Output(Bool())
  val onUp = Output(Bool())
}

class Debounce(implicit options: GenerateOptions) extends Module {
  private val REQUIRED_INTERVAL = 5  // ms
  private val REQUIRED_CYCLES = options.systemClock * REQUIRED_INTERVAL / 1000
  private val REQUIRED_BITS = Helper.log2(REQUIRED_CYCLES)

  val io = IO(new Bundle {
    val in = Input(Bool())
    val out = new DebounceOutBundle()
  })

  val sync = ShiftRegister(io.in, 2)

  val out = RegInit(0.B)
  val idle = out === sync

  // idea: only take the input when it's stable (unchanged) in `REQUIRED_INTERVAL` time.
  // when the input is different from output, increasing the counter
  // when the counter is full, flipped the output
  val cnt = RegInit(0.U(REQUIRED_BITS.W))
  val onMax = cnt.andR
  cnt := Mux(idle, 0.U, cnt + 1.U)
  out := Mux(onMax, !out, out)

  io.out.out := RegNext(out)
  io.out.onDown := RegNext(!idle && onMax && !out)
  io.out.onUp := RegNext(!idle && onMax && out)
}

object Debounce {
  def apply(in: Bool)(implicit options: GenerateOptions): DebounceOutBundle = {
    val debounce = Module(new Debounce())
    debounce.io.in := in
    debounce.io.out
  }
}

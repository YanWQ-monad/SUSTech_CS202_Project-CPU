package board

import util._
import chisel3._
import chisel3.util._

class KeyboardInBundle extends Bundle {
  val rows = Input(Vec(4, Bool()))
  val cols = Input(Vec(4, Bool()))
}

class Keyboard extends Module {
  val io = IO(new Bundle {
    val in = new KeyboardInBundle()  // require denounced
    val out = Decoupled(KeyboardButton())
  })

  val press = RegInit(0.B)
  val out = RegInit(KeyboardButton._1)
  val inputValid = PopCount(io.in.rows) === 1.U && PopCount(io.in.cols) === 1.U

  io.out.bits := RegNext(out)
  io.out.valid := RegNext(inputValid && !press)

  when (io.in.rows.asUInt.orR === 0.U && io.in.cols.asUInt.orR === 0.U) {
    press := 0.B
  }

  when (inputValid) {
    val row = Decoder(4)(io.in.rows.asUInt)
    val col = Decoder(4)(io.in.cols.asUInt)
    val value = Cat(row, col)
    out := KeyboardButton(value)
    io.out.valid := true.B
    press := true.B
  }
}

object KeyboardButton extends ChiselEnum {
  val _1, _2, _3, A,
      _4, _5, _6, B,
      _7, _8, _9, C,
      Star, _0, _Number, D = Value
}

package board

import util.{GenerateOptions, Helper, Split, switch}
import chisel3._
import chisel3.util._

import scala.collection.mutable.ListBuffer

class TubesGroupBundle extends Bundle {
  val enable = Output(Vec(8, Bool()))
  val tubes = Output(Vec(8, Bool()))   // [7:0] PGFEDCBA
}

class TubesInputBundle extends Bundle {
  val value = UInt(32.W)
  val enables = Vec(8, Bool())
  val mode = TubesMode()
  val effect = TubesEffect()  // todo
}

object TubesMode extends ChiselEnum {
  val Dec, Hex = Value
}

object TubesEffect extends ChiselEnum {
  val None, Blink = Value
}

class TubesController(implicit options: GenerateOptions) extends Module {
  val io = IO(new Bundle {
    val in = Input(new TubesInputBundle())
    val out = new TubesGroupBundle()
  })

//  val scale = {
//    val ratio = options.standardClock / 8000
//    Helper.log2(ratio)
//  }
  val scale = 12

  val tubeClock = withReset(0.B.asAsyncReset) {
    // todo: init reg
    val clockCnt = RegInit(0.U(scale.W))
    clockCnt := clockCnt + 1.U
    clockCnt(scale - 1).asClock
  }
  dontTouch(tubeClock)

  withClock(tubeClock) {
    val decDigits = {
      val buffer = ListBuffer.empty[UInt]
      var current = io.in.value
      for (_ <- 0 until 8) {
        val (q, r) = DivTen(current)
        current = q
        buffer += r
      }

      buffer.toSeq
    }

    val hexDigits = Split(io.in.value, 8, 4)

    val digits = Wire(Vec(8, UInt(4.W)))
    switch (io.in.mode)
      .is (TubesMode.Dec) { digits := decDigits }
      .is (TubesMode.Hex) { digits := hexDigits }
      .default { digits := DontCare }

    val tube = Module(new Tubes)

    tube.io.digits := digits.reverse
    tube.io.enables := io.in.enables
    tube.io.dots := 0.U(8.W).asBools
    io.out <> tube.io.out
  }
}

class Tubes extends Module {
  val io = IO(new Bundle {
    val digits = Input(Vec(8, UInt(4.W)))
    val dots = Input(Vec(8, Bool()))
    val enables = Input(Vec(8, Bool()))

    val out = new TubesGroupBundle()
  })

  val now = RegInit(0.U(3.W))
  now := now + 1.U

  val table = Seq(
    0 -> "b10000000",
    1 -> "b01000000",
    2 -> "b00100000",
    3 -> "b00010000",
    4 -> "b00001000",
    5 -> "b00000100",
    6 -> "b00000010",
    7 -> "b00000001",
  ).map((o) => o._1.U(now.getWidth.W) -> o._2.U(8.W))

  io.out.enable := (~(MuxLookup(now, table.head._2)(table) & io.enables.asUInt)).asBools
  io.out.tubes := (~Cat(io.dots(now), BcdToTube(io.digits(now)).asUInt)).asBools
}

class BcdToTube extends Module {
  val io = IO(new Bundle {
    val digit = Input(UInt(4.W))
    val tubes = Output(Vec(7, Bool()))  // [6:0] GFEDCBA
  })

  val table = Seq(
    0x0 -> "b0111111",
    0x1 -> "b0000110",
    0x2 -> "b1011011",
    0x3 -> "b1001111",
    0x4 -> "b1100110",
    0x5 -> "b1101101",
    0x6 -> "b1111101",
    0x7 -> "b0000111",
    0x8 -> "b1111111",
    0x9 -> "b1101111",
    0xA -> "b1110111",
    0xB -> "b1111100",
    0xC -> "b1011000",
    0xD -> "b1011110",
    0xE -> "b1111001",
    0xF -> "b1110001",
  ).map((o) => o._1.U(io.digit.getWidth.W) -> o._2.U(io.tubes.getWidth.W))

  io.tubes := MuxLookup(io.digit, table.head._2)(table).asBools
}

object BcdToTube {
  def apply(digit: UInt): Vec[Bool] = {
    val toTube = Module(new BcdToTube)
    toTube.io.digit := digit
    toTube.io.tubes
  }
}

// todo: spec
class DivTen extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(32.W))
    val quotient = Output(UInt(29.W))
    val remainder = Output(UInt(4.W))
  })

  val q1 = (io.in >> 1).asUInt +& (io.in >> 2).asUInt;
  val q2 = q1 + (q1 >> 4).asUInt
  val q3 = q2 + (q2 >> 8).asUInt
  val q4 = q3 + (q3 >> 16).asUInt
  val q5 = (q4 >> 3).asUInt
  val r = io.in - (((q5 << 2).asUInt + q5) << 1).asUInt

  io.quotient := q5 + (r > 9.U).asUInt
  io.remainder := io.in - (io.quotient << 1).asUInt - (io.quotient << 3).asUInt
}

object DivTen {
  def apply(in: UInt): (UInt, UInt) = {
    val divTen = Module(new DivTen())
    divTen.io.in := in
    (divTen.io.quotient, divTen.io.remainder)
  }
}

//object TempMain extends App {
//  implicit val options = new GenerateOptions(
//    false,
//    false,
//    100_000_000,
//    10_000_000,
//    8_500_000,
//    40_000_000)
//
//  _root_.util.Emit(new TubesController(), args)
//}

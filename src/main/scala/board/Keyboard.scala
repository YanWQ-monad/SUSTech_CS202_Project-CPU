package board

import util.{Counter, _}
import chisel3._
import chisel3.util._

class KeyboardInBundle extends Bundle {
  val rows = Output(Vec(4, Bool()))
  val cols = Input(Vec(4, Bool()))
}

class Keyboard(implicit options: GenerateOptions) extends Module {
  private val REQUIRED_INTERVAL = 1 // ms
  private val REQUIRED_CYCLES = options.systemClock * REQUIRED_INTERVAL / 1000
  private val REQUIRED_BITS = Helper.log2(REQUIRED_CYCLES)

  val io = IO(new Bundle {
    val in = new KeyboardInBundle()  // require denounced
    val out = Decoupled(KeyboardButton())
  })

  // the columns data for each row
  val rows = RegInit(VecInit.fill(4)(0.U(4.W)))

  // because debounce in keyboard is hard, so we slow down the scan speed to achieve debounce
  // here, we scan every line in every 2^REQUIRED_CYCLES cycles.
  val tick = Pulse(Counter.maxBits(REQUIRED_BITS))
  val cnt = Counter.maxBits(2, tick)
  io.in.rows := RegNext(~Encoder(4)(cnt)).asTypeOf(UInt(4.W)).asBools
  when (tick) { rows(cnt) := ~io.in.cols.asUInt }


  // record whether the key is pressed, avoiding multiple events
  val press = RegInit(false.B)
  // if no rows has key pressed, then reset `press`
  when (!rows.asUInt.orR) {
    press := false.B
  }

  // now, we scan each line, to decode which key is pressed
  val state = RegInit(KeyboardState.Row1)
  state := MuxLookup(state, KeyboardState.Row1)(KeyboardState.nextTable())

  val currentRow = rows(state.asUInt)
  // `onDown`: tell whether `currentRow` has new key pressed (so rule out that `press` is true)
  // note: `onDown` will be high in only one cycle
  val onDown = !press && currentRow.orR
  val out = RegInit(KeyboardButton._1)

  io.out.bits := out
  io.out.valid := RegNext(onDown)  // delay one cycle, to match `io.out.bits`

  when (onDown) {
    val row = state.asUInt
    val col = Decoder(4)(currentRow)
    out := KeyboardButton(Cat(row, col))  // decode the key by simple concat
    press := true.B
  }
}

object KeyboardState extends ChiselEnum {
  val Row1 = Value(0.U)
  val Row2 = Value(1.U)
  val Row3 = Value(2.U)
  val Row4 = Value(3.U)

  def nextTable(): Seq[(Type, Type)] = Seq(
    Row1 -> Row2,
    Row2 -> Row3,
    Row3 -> Row4,
    Row4 -> Row1,
  )
}

object KeyboardButton extends ChiselEnum {
  val D, _Number, _0, Star,
      C, _9, _8, _7,
      B, _6, _5, _4,
      A, _3, _2, _1 = Value
}

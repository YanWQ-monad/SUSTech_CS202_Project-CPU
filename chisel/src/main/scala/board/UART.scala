package board

import chisel3._
import chisel3.util._

class UARTPair extends Bundle {
  val tx = Output(Bool())
  val rx = Input(Bool())
}

class UART(dataBits: Int, oversampleLog: Int) extends Module {
  val io = IO(new Bundle {
    val pair = new UARTPair()

    val dataIn = Flipped(Decoupled(UInt(dataBits.W)))
    val dataOut = Decoupled(UInt(dataBits.W))
  })

  val tx = Module(new UARTTransmitter(dataBits))
  val rx = Module(new UARTReceiver(dataBits, oversampleLog))

  rx.rx := io.pair.rx
  rx.io <> io.dataOut
  rx.io.ready := DontCare
  tx.io <> io.dataIn
  io.pair.tx := tx.tx

  // transmitter doesn't not need oversample, so we need to slow down its clock (by ticking)
  val clkCnt = RegInit(0.U(oversampleLog.W))
  clkCnt := clkCnt + 1.U
  tx.tick := clkCnt.andR
}

class UARTTransmitter(dataBits: Int) extends Module {
  val io = IO(Flipped(Decoupled(UInt(dataBits.W))))
  val tx = IO(Output(Bool()))
  val tick = IO(Input(Bool()))

  val state = RegInit(UARTState.Idle)
  val data = Reg(UInt(dataBits.W))
  val idle = state === UARTState.Idle

  when (tick) {
    val idleNext = Mux(io.valid, UARTState.Start, UARTState.Idle)
    val nextState = MuxLookup(state, idleNext)(UARTState.nextTable())
    state := nextState

    data := Mux(state === UARTState.Start && io.valid, io.bits, data >> 1)
  }

  val onBit = state.asUInt >= 4.U  // is sending a bit (compared to Start, Stop1, and Stop2)
  // if `onBit`, send bit (from LSB), otherwise send control signal
  tx := Mux(onBit, data(0), Mux(state === UARTState.Start, 0.B, 1.B))

  // to tell whether the data is sent, used for popping the head data from the queue
  io.ready := tick && state === UARTState.Start
}

class UARTReceiver(dataBits: Int, oversampleLog: Int) extends Module {
  val io = IO(Decoupled(UInt(dataBits.W)))
  val rx = IO(Input(Bool()))

  val initialCnt = (1 << (oversampleLog - 1)) - 2
  val cnt = RegInit(initialCnt.U((oversampleLog - 1).W))

  val sync = ShiftRegister(rx, 2)
  val bit = RegInit(true.B)

  when (sync && !cnt.andR) {
    cnt := cnt + 1.U
  } .elsewhen (!sync && cnt =/= 0.U) {
    cnt := cnt - 1.U
  }

  // take the majority bits as the final bit
  when (cnt.andR) { bit := 1.U }
    .elsewhen (cnt === 0.U) { bit := 0.U }

  // -------------|     START     |--------------
  //     IDLE     |---------------|     BIT0
  // spacing:  0000000123456789ABCDEF0123
  // cnt:      333321000..........012333.....
  // bit:      111111000......000..00111.....
  //                 ^         ^
  //                 |      we take the final bit here. If we take it in spacing=F, there will be more errors
  //                cnt to 0, spacing starts

  val spacing = RegInit(0.U(oversampleLog.W))
  val tick = spacing === ((1 << oversampleLog) - oversampleLog * 3 / 2).U  // tell should we take the bit

  val state = RegInit(UARTState.Idle)
  val isIdle = state === UARTState.Idle

  when (isIdle) {
    state := Mux(!bit, UARTState.Start, UARTState.Idle)
  } .otherwise {
    state := Mux(spacing.andR, MuxLookup(state, UARTState.Idle)(UARTState.nextTable()), state)
  }

  spacing := Mux(isIdle, 0.U, spacing + 1.U)

  val data = ShiftRegisters(bit, 8, tick)  // when `tick`, take the bit as LSB, and shift left
  io.bits := Cat(data).asUInt

  // only valid for 1 cycles when the data is ready, tell the queue to push
  io.valid := false.B
  when (state === UARTState.Stop1 && tick) {
    io.valid := true.B
  }
}

// Idle -> Start -> Bit0 -> Bit1 -> ... -> Bit7 -> Stop1 -> Idle(Stop2) -> Start -> Bit0
// note that, when sending continuously, Idle is equivalent to Stop2
object UARTState extends ChiselEnum {
  val Idle = Value(0.U)
  val Start = Value(1.U)
  val Stop1 = Value(2.U)
  val Stop2 = Value(3.U)  // emit
  val Bit0, Bit1, Bit2, Bit3, Bit4, Bit5, Bit6, Bit7 = Value
  //   4     5     6     7     8     9     A     B

  def nextTable(): Seq[(Type, Type)] = Array(
      Start -> Bit0,
      Bit0 -> Bit1,
      Bit1 -> Bit2,
      Bit2 -> Bit3,
      Bit3 -> Bit4,
      Bit4 -> Bit5,
      Bit5 -> Bit6,
      Bit6 -> Bit7,
      Bit7 -> Stop1,
      Stop1 -> Idle,
    )
}

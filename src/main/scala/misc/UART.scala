package misc

import chisel3._
import chisel3.util._

class UART(dataBits: Int) extends Module {
  val io = IO(new Bundle {
    val tx = Output(Bool())
    val rx = Input(Bool())

    val dataIn = Flipped(Decoupled(UInt(dataBits.W)))
    val dataOut = Decoupled(UInt(dataBits.W))
  })

  val tx = Module(new UARTTransmitter(dataBits))
  val rx = Module(new UARTReceiver(dataBits))

  rx.rx := io.rx
  rx.io <> io.dataOut
  rx.io.ready := DontCare
  tx.io <> io.dataIn
  io.tx := tx.tx

  val clkCnt = RegInit(0.U(3.W))
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

  val onBit = state.asUInt >= 4.U
  val dataBit = data(0)
  tx := Mux(onBit, dataBit, Mux(state === UARTState.Start, 0.B, 1.B))
  io.ready := tick && state === UARTState.Start
}

// 8 times oversample
class UARTReceiver(dataBits: Int) extends Module {
  val io = IO(Decoupled(UInt(dataBits.W)))
  val rx = IO(Input(Bool()))

  val sync = ShiftRegister(rx, 2)
  val cnt = RegInit(2.U(2.W))
  val bit = RegInit(true.B)

  when (sync && cnt =/= "b11".U) {
    cnt := cnt + 1.U
  } .elsewhen (!sync && cnt =/= "b00".U) {
    cnt := cnt - 1.U
  }

  when (cnt.andR) { bit := 1.U }
    .elsewhen (!cnt.orR) { bit := 0.U }

  val clkCnt = RegInit(0.U(3.W))
  clkCnt := clkCnt + 1.U
  val tick = clkCnt.andR

  val state = RegInit(UARTState.Idle)
  val idleNext = Mux(!bit, UARTState.Start, UARTState.Idle)
  val nextState = Mux(tick, MuxLookup(state, idleNext)(UARTState.nextTable()), state)
  state := nextState

  val data = ShiftRegisters(bit, 8, tick)
  io.bits := Cat(data).asUInt

  io.valid := false.B
  when (state === UARTState.Bit7 && tick) {
    io.valid := true.B
  }
}

object UARTState extends ChiselEnum {
  val Idle = Value(0.U)
  val Start = Value(1.U)
  val Stop1 = Value(2.U)
  val Stop2 = Value(3.U)
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

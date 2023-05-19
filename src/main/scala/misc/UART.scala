package misc

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode.TruthTable

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

  val tx = Module(new UARTTransmitter(dataBits, oversampleLog))
  val rx = Module(new UARTReceiver(dataBits, oversampleLog))

  rx.rx := io.pair.rx
  rx.io <> io.dataOut
  rx.io.ready := DontCare
  tx.io <> io.dataIn
  io.pair.tx := tx.tx

  val clkCnt = RegInit(0.U(oversampleLog.W))
  clkCnt := clkCnt + 1.U
  tx.tick := clkCnt.andR
}

class UARTTransmitter(dataBits: Int, oversampleLog: Int) extends Module {
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

// 16 times oversample
class UARTReceiver(dataBits: Int, oversampleLog: Int) extends Module {
  val io = IO(Decoupled(UInt(dataBits.W)))
  val rx = IO(Input(Bool()))

  val initialCnt = (1 << oversampleLog) - 2
  val cnt = RegInit(initialCnt.U((oversampleLog - 1).W))

  val sync = ShiftRegister(rx, 2)
  val bit = RegInit(true.B)

  when (sync && !cnt.andR) {
    cnt := cnt + 1.U
  } .elsewhen (!sync && cnt =/= 0.U) {
    cnt := cnt - 1.U
  }

  when (cnt.andR) { bit := 1.U }
    .elsewhen (cnt === 0.U) { bit := 0.U }

  val spacing = RegInit(0.U(oversampleLog.W))
  val tick = spacing === ((1 << oversampleLog) - oversampleLog * 3 / 2).U

  val state = RegInit(UARTState.Idle)
  val isIdle = state === UARTState.Idle

  when (isIdle) {
    state := Mux(!bit, UARTState.Start, UARTState.Idle)
  } .otherwise {
    state := Mux(spacing.andR, MuxLookup(state, UARTState.Idle)(UARTState.nextTable()), state)
  }

  spacing := Mux(isIdle, 0.U, spacing + 1.U)

  val data = ShiftRegisters(bit, 8, tick)
  io.bits := Cat(data).asUInt

  io.valid := false.B
  when (state === UARTState.Stop1 && tick) {
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

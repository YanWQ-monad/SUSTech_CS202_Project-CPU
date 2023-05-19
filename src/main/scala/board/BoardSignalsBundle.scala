package board

import misc._
import util.GenerateOptions
import chisel3._
import chisel3.util._

class BoardDataRegableBundle extends Bundle {
  val tubes = new TubesInputBundle()
}

class BoardDataBundle extends Bundle {
  val uartOut = Flipped(Decoupled(UInt(8.W)))
  val uartIn = Decoupled(UInt(8.W))
  val cycles = Output(UInt(32.W))
  val regable = Input(new BoardDataRegableBundle())
}

class RealBoardDataBundle extends Bundle {
  val uart = new UARTPair()
  val tubes = new TubesGroupBundle()
}

class BoardDataController(implicit options: GenerateOptions) extends Module {
  val outer = IO(new RealBoardDataBundle())
  val inside = IO(new BoardDataBundle())
  val io = IO(new Bundle {
    val uartClock = Input(Clock())
    val cycles = Input(UInt(32.W))
  })

  inside.cycles := io.cycles

  val uart = Module(new UARTWrap(8, 4))
  uart.io.pair <> outer.uart
  uart.io.dataTx <> inside.uartOut
  uart.io.dataRx <> inside.uartIn
  uart.io.uartClock := io.uartClock

  val tube = Module(new TubesController())
  tube.io.out <> outer.tubes
  tube.io.in <> inside.regable.tubes
}

class UARTWrap(dataBits: Int, oversampleLog: Int) extends Module {
  val io = IO(new Bundle {
    val pair = new UARTPair()

    val dataTx = Flipped(Decoupled(UInt(dataBits.W)))
    val dataRx = Decoupled(UInt(dataBits.W))

    val uartClock = Input(Clock())
  })

  val txQueue = Module(new CrossClockQueue(UInt(dataBits.W), 128))  // CPU  --> UART
  val rxQueue = Module(new CrossClockQueue(UInt(dataBits.W), 128))  // UART --> CPU

  val uart = withClock(io.uartClock) { Module(new UART(dataBits, oversampleLog)) }

  uart.io.pair <> io.pair

  txQueue.io.clkEnq := clock
  txQueue.io.clkDeq := io.uartClock
  txQueue.io.enq <> io.dataTx
  txQueue.io.deq <> uart.io.dataIn

  rxQueue.io.clkEnq := io.uartClock
  rxQueue.io.clkDeq := clock
  rxQueue.io.enq <> uart.io.dataOut
  rxQueue.io.deq <> io.dataRx
}

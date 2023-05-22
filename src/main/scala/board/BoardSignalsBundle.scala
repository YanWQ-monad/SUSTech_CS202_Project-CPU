package board

import board.display._
import misc._
import util.GenerateOptions
import chisel3._
import chisel3.util._
import ip._

class BoardDataRegableBundle extends Bundle {
  val tubes = new TubesInputBundle()
  val leds = Vec(24, Bool())
}

class ButtonGroupBundle extends Bundle {
  val up = Input(Bool())
  val down = Input(Bool())
  val left = Input(Bool())
  val right = Input(Bool())
  val center = Input(Bool())
}

class BoardDataBundle extends Bundle {
  val uartOut = Flipped(Decoupled(UInt(8.W)))
  val uartIn = Decoupled(UInt(8.W))
  val cycles = Output(UInt(32.W))
  val regable = Input(new BoardDataRegableBundle())
  val switches = Output(Vec(24, Bool()))
  val buttons = Flipped(new ButtonGroupBundle())
  val keyboard = Decoupled(KeyboardButton())
  val vgaDataPort = new MemoryPort(14, 16)
  val originPC = Output(UInt(32.W))
}

class RealBoardDataBundle extends Bundle {
  val uart = new UARTPair()
  val tubes = new TubesGroupBundle()
  val vga = new VGAOutBundle()
  val switches = Input(Vec(24, Bool()))
  val buttons = new ButtonGroupBundle()
  val keyboard = new KeyboardInBundle()
  val leds = Output(Vec(24, Bool()))
  val interrupt = Input(Bool())
}

class BoardDataController(implicit options: GenerateOptions) extends Module {
  val outer = IO(new RealBoardDataBundle())
  val inside = IO(new BoardDataBundle())
  val io = IO(new Bundle {
    val uartClock = Input(Clock())
    val vgaClock = Input(Clock())
    val cpuClock = Input(Clock())
    val cycles = Input(UInt(32.W))

    val originPC = Input(UInt(32.W))
    val interrupt = Output(Bool())
  })

  inside.cycles := io.cycles
  inside.originPC := io.originPC
  io.interrupt := withClock (io.cpuClock) { Debounce(outer.interrupt).onDown }

  inside.switches <> outer.switches.map(Debounce(_).out)
  inside.buttons.elements.foreach(pair => pair._2 := Debounce(outer.buttons.elements(pair._1).asUInt.asBool).out)
  inside.regable.leds <> outer.leds

  val uart = Module(new UARTWrap(8, 4))
  uart.io.pair <> outer.uart
  uart.io.dataTx <> inside.uartOut
  uart.io.dataRx <> inside.uartIn
  uart.io.uartClock := io.uartClock

  val tube = Module(new TubesController())
  tube.io.out <> outer.tubes
  tube.io.in <> inside.regable.tubes

  val keyboard = Module(new Keyboard())
  val keyboardQueue = Module(new CrossClockQueue(KeyboardButton(), 32))
  keyboard.io.in <> outer.keyboard
  keyboardQueue.io.clkEnq := clock
  keyboardQueue.io.clkDeq := clock
  keyboardQueue.io.enq <> keyboard.io.out
  keyboardQueue.io.deq <> inside.keyboard

  val display = Module(new DisplaySubsystem())
  display.io.vgaClock := io.vgaClock
  display.io.vgaDataPort <> inside.vgaDataPort
  outer.vga := RegNext(display.io.out)
}

class DisplaySubsystem(implicit options: GenerateOptions) extends Module {
  val vgaParams = VGAParams(
    new OneTiming(800, 40, 128, 88),  // horizontal
    new OneTiming(600, 1, 4, 23),  // vertical
    11)

  val io = IO(new Bundle {
    val vgaClock = Input(Clock())
    val out = new VGAOutBundle()
    val vgaDataPort = new MemoryPort(14, 16)
  })

  val vga = withClock(io.vgaClock) { Module(new VGATop(vgaParams)) }
  val console = withClock(io.vgaClock) { Module(new Console(vgaParams)) }
  val dataMem = Module(new BlockMemory(1, 256 * 64, ConsoleCharBundle.packedWidth))
  val fontRom = withClock(io.vgaClock) { Module(new BlockMemoryRom(2, 0x100 * 16, 8, Some("font.txt"))) }
  dataMem.io.clockB := io.vgaClock
  dataMem.io2.init()
  fontRom.io.init()

  require(dataMem.addrWidth == io.vgaDataPort.addrWidth)
  require(dataMem.dataWidth == io.vgaDataPort.dataWidth)

  vga.io.out <> io.out

  console.io.charRamData := dataMem.io2.setRead(console.io.charRamAddr)
  console.io.fontRomData := fontRom.io.setRead(console.io.fontRomAddr)
  console.io.out <> vga.io.in
  console.io.info <> vga.io.info
  io.vgaDataPort <> dataMem.io1
}

class UARTWrap(dataBits: Int, oversampleLog: Int) extends Module {
  val io = IO(new Bundle {
    val pair = new UARTPair()

    val dataTx = Flipped(Decoupled(UInt(dataBits.W)))
    val dataRx = Decoupled(UInt(dataBits.W))

    val uartClock = Input(Clock())
  })

  val txQueue = Module(new CrossClockQueue(UInt(dataBits.W), 128)) // CPU  --> UART
  val rxQueue = Module(new CrossClockQueue(UInt(dataBits.W), 128)) // UART --> CPU

  val uart = withClock(io.uartClock) {
    Module(new UART(dataBits, oversampleLog))
  }

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

package core

import util.{switch, _}
import board._
import misc._
import board.display.ConsoleCharBundle
import ip._
import chisel3._
import chisel3.util._

class Memory(bytes: BigInt)(implicit options: GenerateOptions) extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val memWidth = Input(MemWidth())
    val write = Input(Bool())
    val willWrite = Input(Bool())
    val enable = Input(Bool())
    val dataIn = Input(SInt(32.W))
    val unsigned = Input(Bool())
    val stage = Input(Bool())

    val dataOut = Output(SInt(32.W))

    val addrPC = Input(UInt(32.W))
    val dataPC = Output(UInt(32.W))

    val external = Flipped(new BoardDataBundle())
  })

  val inner = Module(new MemoryDispatch(bytes))

  io.external <> inner.io.external

  io.dataPC := inner.io.dataPC
  inner.io.addrPC := io.addrPC

  inner.io.stage := io.stage
  inner.io.enable := io.enable
  inner.io.write := io.write
  inner.io.willWrite := io.willWrite
  inner.io.addr := io.addr

  val dataInVec = Split(io.dataIn.asUInt, 4, 8)

  io.dataOut := DontCare
  inner.io.dataIn := DontCare
  when (io.enable) {
    // to support more general memory
    // if we need to write a sub-word (i.e. byte or half-word)
    // we first read the whole word from memory (by 4-byte-address)
    // then modify the bytes of it, and write it back to the memory

    val dataOut = inner.io.dataOut
    val port = Split(dataOut, 4, 8)

    val halfX = Cat(io.addr(1), 1.U(1.W))  // high byte index of half-word
    val halfY = Cat(io.addr(1), 0.U(1.W))  // low byte index of half-word

    when (io.write) {
      switch (io.memWidth)
        .is (MemWidth.Word) { port := dataInVec }
        .is (MemWidth.Half) {
          port(halfX) := dataInVec(1)
          port(halfY) := dataInVec(0)
        }
        .is (MemWidth.Byte) { port(io.addr(1, 0)) := dataInVec(0) }

      inner.io.dataIn := port.asUInt  // finally, take the modified word as input for write
    } .otherwise {
      switch (io.memWidth)
        .is (MemWidth.Word) { io.dataOut := port.asUInt.asSInt }
        .is (MemWidth.Half) {
          val interim = Cat(port(halfX), port(halfY))
          io.dataOut := Mux(io.unsigned, ZeroExt32(interim), SignExt32(interim)).asSInt
        }
        .is (MemWidth.Byte) {
          val interim = port(io.addr(1, 0))
          io.dataOut := Mux(io.unsigned, ZeroExt32(interim), SignExt32(interim)).asSInt
        }
    }
  }
}

// dispatch the address to external IO or the "true" RAM
class MemoryDispatch(bytes: BigInt)(implicit options: GenerateOptions) extends Module {
  def vgaDataRW(port: MemoryPort, addr: UInt, write: Bool, getter: (ConsoleCharBundle) => UInt, updater: (ConsoleCharBundle) => Unit): UInt = {
    val rawData = port.setRead(addr)

    val data = ConsoleCharBundle.unpack(rawData)
    updater(data)
    port.setWrite(addr, data.pack, write)

    getter(ConsoleCharBundle.unpack(rawData))
  }

  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val write = Input(Bool())
    val willWrite = Input(Bool())
    val enable = Input(Bool())
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
    val stage = Input(Bool())

    val addrPC = Input(UInt(32.W))
    val dataPC = Output(UInt(32.W))

    val external = Flipped(new BoardDataBundle())
  })

  val mem = Module(new BlockMemory(0, bytes, 32, Some("main.txt")))
  mem.io1.init()  // init: fill the default inputs
  mem.io2.init()
  mem.io.clockB := clock

  io.dataPC := mem.io1.setRead((io.addrPC >> 2).asUInt)
  io.dataOut := DontCare

  val dequeueData = RegInit(0.U(32.W))  // used for store dequeued data
  // `externalReg`: the buffer that with data for external output, e.g. tubes, LEDs
  val externalReg = RegInit(0.U.asTypeOf(new BoardDataRegableBundle))
  // dequeue enable should be careful, only dequeue when write is disable and stage=0
  val shouldReadDequeue = io.enable && !io.willWrite && !io.stage
  val randomValue = RNG(32)

  io.external.regable := externalReg
  io.external.uartOut.noenq()  // noenq, nodeq, init: set default input (i.e. tell to do nothing)
  io.external.uartIn.nodeq()
  io.external.keyboard.nodeq()
  io.external.vgaDataPort.init()

  io.dataOut := 0.U
  // external IO is 4-byte aligned, although most of them are actually byte
  switch (io.addr)
    .is (0xFFFFF000L.U) {  // read UART
      when(shouldReadDequeue && io.external.uartIn.valid) {
        dequeueData := io.external.uartIn.deq()
      }
      io.dataOut := dequeueData
    }
    .is (0xFFFFF004L.U) {  // write UART
      when(io.enable && io.write && io.external.uartOut.ready) {
        io.external.uartOut.enq(io.dataIn(7, 0))
      }
    }
    .is (0xFFFFF008L.U) { io.dataOut := io.external.uartIn.valid }
    .is (0xFFFFF00CL.U) { io.dataOut := io.external.uartOut.ready }
    .is (0xFFFFF010L.U) { io.dataOut := io.external.cycles }
    .is (0xFFFFF014L.U) {
      when(shouldReadDequeue && io.external.keyboard.valid) {
        dequeueData := io.external.keyboard.deq().asUInt
      }
      io.dataOut := dequeueData
    }
    .is (0xFFFFF018L.U) { io.dataOut := io.external.keyboard.valid }
    .is (0xFFFFF01CL.U) { io.dataOut := io.external.buttons.center }
    .is (0xFFFFF020L.U) { io.dataOut := io.external.buttons.up }
    .is (0xFFFFF024L.U) { io.dataOut := io.external.buttons.down }
    .is (0xFFFFF028L.U) { io.dataOut := io.external.buttons.left }
    .is (0xFFFFF02CL.U) { io.dataOut := io.external.buttons.right }
    .is (0xFFFFF030L.U) { externalReg.tubes.mode := TubesMode(io.dataIn(TubesMode.getWidth - 1, 0)) }
    .is (0xFFFFF034L.U) { externalReg.tubes.effect := TubesEffect(io.dataIn(TubesEffect.getWidth - 1, 0)) }
    .is (0xFFFFF038L.U) { externalReg.tubes.value := io.dataIn }
    .is (0xFFFFF03CL.U) { externalReg.tubes.enables := io.dataIn(7, 0).asBools }
    .is (0xFFFFF040L.U) { io.dataOut := io.external.originPC }
    .is (0xFFFFF044L.U) { io.dataOut := randomValue }
    .default {
      when(io.addr(31, 16) === "b1111111111111101".U) {  // screen chars data
        val addr = io.addr(15, 2)
        io.dataOut := vgaDataRW(io.external.vgaDataPort, addr, io.write,
          bundle => bundle.char, bundle => bundle.char := io.dataIn(bundle.char.getWidth - 1, 0))
      } .elsewhen(io.addr(31, 16) === "b1111111111111110".U) {  // screen color data
        val addr = io.addr(15, 2)
        io.dataOut := vgaDataRW(io.external.vgaDataPort, addr, io.write,
          bundle => Cat(bundle.colorBg, bundle.color),
          bundle => {
            bundle.colorBg := io.dataIn(bundle.colorBg.getWidth + bundle.color.getWidth - 1, bundle.color.getWidth)
            bundle.color := io.dataIn(bundle.color.getWidth - 1, 0)
          })
      } .elsewhen(io.addr(31, 8) === 0xFFFFF1.U) {  // switches
        val index = io.addr(7, 2)
        when(index < 24.U) {
          io.dataOut := io.external.switches(index)
        }
      } .elsewhen(io.addr(31, 8) === 0xFFFFF2.U) {  // LEDs
        val index = io.addr(7, 2)
        when(index < 24.U) {
          externalReg.leds(index) := io.dataIn(0)
        }
      } .otherwise {
        val addr = (io.addr >> 2).asUInt
        when(io.enable) {
          io.dataOut := mem.io2.setRead(addr)
          mem.io2.setWrite(addr, io.dataIn, io.write)
        }
      }
    }
}

object MemWidth extends ChiselEnum {
  val Byte, Half, Word = Value
}

package core

import util.{switch, _}
import misc._
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
    val step = Input(Bool())

    val dataOut = Output(SInt(32.W))

    val addrPC = Input(UInt(32.W))
    val dataPC = Output(UInt(32.W))

    val external = Flipped(new BoardDataBundle())
  })

  val inner = Module(new MemoryDispatch(bytes))

  io.external <> inner.io.external

  io.dataPC := inner.io.dataPC
  inner.io.addrPC := io.addrPC

  inner.io.step := io.step
  inner.io.enable := io.enable
  inner.io.write := io.write
  inner.io.willWrite := io.willWrite
  inner.io.addr := io.addr

  val dataInVec = Split(io.dataIn.asUInt, 4, 8)

  io.dataOut := DontCare
  inner.io.dataIn := DontCare
  when (io.enable) {
    val dataOut = inner.io.dataOut
    val port = Split(dataOut, 4, 8)

    val halfX = Cat(io.addr(1), 1.U(1.W))
    val halfY = Cat(io.addr(1), 0.U(1.W))

    when (io.write) {
      switch (io.memWidth)
        .is (MemWidth.Word) { port := dataInVec }
        .is (MemWidth.Half) {
          port(halfX) := dataInVec(1)
          port(halfY) := dataInVec(0)
        }
        .is (MemWidth.Byte) { port(io.addr(1, 0)) := dataInVec(0) }

      inner.io.dataIn := port.asUInt
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

class MemoryDispatch(bytes: BigInt)(implicit options: GenerateOptions) extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val write = Input(Bool())
    val willWrite = Input(Bool())
    val enable = Input(Bool())
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
    val step = Input(Bool())

    val addrPC = Input(UInt(32.W))
    val dataPC = Output(UInt(32.W))

    val external = Flipped(new BoardDataBundle())
  })

  val mem = Module(new BlockMemory(bytes, 32))
  mem.io1.init()
  mem.io2.init()

  io.dataPC := mem.io1.setRead((io.addrPC >> 2).asUInt)
  io.dataOut := DontCare

  val uartData = RegInit(0.U(32.W))
  val externalReg = RegInit(0.U.asTypeOf(new BoardDataRegableBundle))

  io.external.regable := externalReg
  io.external.uartOut.noenq()
  io.external.uartIn.nodeq()

  io.dataOut := 0.U
  switch (io.addr)
    .is (0xFFFFF000L.U) {
      when(io.enable && io.write && io.external.uartOut.ready) {
        io.external.uartOut.enq(io.dataIn(7, 0))
      }
      when(io.enable && !io.willWrite && !io.step && io.external.uartIn.valid) {
        uartData := io.external.uartIn.deq()
      }
      io.dataOut := uartData
    }
    .is (0xFFFFF004L.U) { io.dataOut := io.external.uartIn.valid }
    .is (0xFFFFF008L.U) { io.dataOut := io.external.uartOut.ready }
    .is (0xFFFFF010L.U) { io.dataOut := io.external.cycles }
    .is (0xFFFFF020L.U) { externalReg.tubes.mode := TubesMode(io.dataIn(TubesMode.getWidth - 1, 0)) }
    .is (0xFFFFF024L.U) { externalReg.tubes.effect := TubesEffect(io.dataIn(TubesEffect.getWidth - 1, 0)) }
    .is (0xFFFFF028L.U) { externalReg.tubes.value := io.dataIn }
    .is (0xFFFFF02CL.U) { externalReg.tubes.enables := io.dataIn(7, 0).asBools }
    .default {
      // .is(0xFFFFFF1.U) {
      //   when(io.addr(7)) { // write
      //     val data = externalOutReg(io.addr(5, 2))
      //     io.dataOut := data
      //     data := io.dataIn
      //   }.otherwise {
      //     io.dataOut := io.externalIn(io.addr(5, 2))
      //   }
      // }
      val addr = (io.addr >> 2).asUInt
      when(io.enable) {
        io.dataOut := mem.io2.setRead(addr)
        mem.io2.setWrite(addr, io.dataIn, io.write)
      }
    }
}

object MemWidth extends ChiselEnum {
  val Byte, Half, Word = Value
}

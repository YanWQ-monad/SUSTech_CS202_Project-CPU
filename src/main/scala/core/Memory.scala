package core

import util._
import chisel3._
import chisel3.util.{switch, _}
import ip.BlockMemory

class Memory(bytes: BigInt)(implicit debugMode: Boolean) extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val memWidth = Input(MemWidth())
    val write = Input(Bool())
    val enable = Input(Bool())
    val dataIn = Input(SInt(32.W))
    val unsigned = Input(Bool())

    val dataOut = Output(SInt(32.W))

    val addrPC = Input(UInt(32.W))
    val dataPC = Output(UInt(32.W))

    val externalIn = Input(Vec(16, UInt(32.W)))
    val externalOut = Output(Vec(16, UInt(32.W)))
  })

  val inner = Module(new MemoryDispatch(bytes))

  io.dataPC := inner.io.dataPC
  inner.io.addrPC := io.addrPC
  io.externalOut := inner.io.externalOut
  inner.io.externalIn := io.externalIn

  inner.io.enable := io.enable
  inner.io.write := io.write
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
      switch (io.memWidth) {
        is (MemWidth.Word) { port := dataInVec }
        is (MemWidth.Half) {
          port(halfX) := dataInVec(1)
          port(halfY) := dataInVec(0)
        }
        is (MemWidth.Byte) { port(io.addr(1, 0)) := dataInVec(0) }
      }

      inner.io.dataIn := port.asUInt
    } .otherwise {
      switch (io.memWidth) {
        is (MemWidth.Word) { io.dataOut := port.asUInt.asSInt }
        is (MemWidth.Half) {
          val interim = Cat(port(halfX), port(halfY))
          io.dataOut := Mux(io.unsigned, ZeroExt32(interim), SignExt32(interim)).asSInt
        }
        is (MemWidth.Byte) {
          val interim = port(io.addr(1, 0))
          io.dataOut := Mux(io.unsigned, ZeroExt32(interim), SignExt32(interim)).asSInt
        }
      }
    }
  }
}

class MemoryDispatch(bytes: BigInt)(implicit debugMode: Boolean) extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val write = Input(Bool())
    val enable = Input(Bool())
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))

    val addrPC = Input(UInt(32.W))
    val dataPC = Output(UInt(32.W))

    val externalIn = Input(Vec(16, UInt(32.W)))
    val externalOut = Output(Vec(16, UInt(32.W)))
  })

  val mem = Module(new BlockMemory(bytes, 32))
  mem.io1.init()
  mem.io2.init()

  io.dataPC := mem.io1.setRead((io.addrPC >> 2).asUInt)
  io.dataOut := DontCare

  val externalOutReg = RegInit(VecInit(Seq.fill(16)(0.U(32.W))))
  io.externalOut := externalOutReg

  when (io.addr(31, 8) === 0xFFFFFF.U) {
    when (io.addr(7)) {  // write
      val data = externalOutReg(io.addr(5, 2))
      io.dataOut := data
      data := io.dataIn
    } .otherwise {
      io.dataOut := io.externalIn(io.addr(5, 2))
    }
  } .otherwise {
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

object TempMain extends App {
  implicit val debugMode = true
  Emit(new Memory((32)), args)
}

package core

import util.{Emit, SignExt32, ZeroExt32}
import chisel3._
import chisel3.util._

class Memory(bytes: BigInt) extends Module {
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
  })

  val mem = Mem(bytes / 4, UInt(32.W))
  chisel3.util.experimental.loadMemoryFromFileInline(mem, "main.txt")

  io.dataPC := mem.read((io.addrPC >> 2).asUInt).asUInt

  val dataInVec = Wire(Vec(4, UInt(8.W)))
  for (i <- 0 until 4)
    dataInVec(i) := io.dataIn.asUInt(i * 8 + 7, i * 8)

  io.dataOut := DontCare
  when (io.enable) {
    val addr = (io.addr >> 2).asUInt

    val dataOut = mem.read(addr)
    val port = Wire(Vec(4, UInt(8.W)))
    for (i <- 0 until 4)
      port(i) := dataOut.asUInt(i * 8 + 7, i * 8)

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

      mem.write(addr, port.asUInt)
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

class MemoryOld(bytes: BigInt) extends Module {
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
  })

  val mem = Mem(bytes / 4, Vec(4, UInt(8.W)))
  // chisel3.util.experimental.loadMemoryFromFileInline(mem, "main.txt")

  io.dataPC := mem.read((io.addrPC >> 2).asUInt).asUInt

  val dataInVec = Wire(Vec(4, UInt(8.W)))
  for (i <- 0 until 4)
    dataInVec(i) := io.dataIn.asUInt(i * 8 + 7, i * 8)

  io.dataOut := DontCare
  when (io.enable) {
    val addr = (io.addr >> 2).asUInt
    val port = mem(addr)

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

object MemWidth extends ChiselEnum {
  val Byte, Half, Word = Value
}

object TempMain extends App {
  Emit(new Memory((32)), args)
}

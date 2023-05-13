package core

import util.Emit
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class RegisterFile extends Module {
  val io = IO(new Bundle {
    val rs1Addr = Input(UInt(5.W))
    val rs2Addr = Input(UInt(5.W))
    val rdAddr = Input(UInt(5.W))
    val rdDataIn = Input(SInt(32.W))
    val write = Input(Bool())

    val rs1Out = Output(SInt(32.W))
    val rs2Out = Output(SInt(32.W))

    val debugAddr = Input(UInt(5.W))
    val debugData = Output(SInt(32.W))
  })

  val mem = Mem(32, SInt(32.W))

  io.debugData := mem.read(io.debugAddr)

  io.rs1Out := mem.read(io.rs1Addr)
  io.rs2Out := mem.read(io.rs2Addr)
  when (io.write && (io.rdAddr =/= 0.U)) {
    mem.write(io.rdAddr, io.rdDataIn)
  }
}


//class SyncReadMemTestWrapper extends Module {
//  val io = IO(new Bundle {
//    val enable = Input(Bool())
//    val write = Input(Bool())
//    val addr = Input(UInt(5.W))
//    val dataIn = Input(SInt(32.W))
//    val dataOut = Output(SInt(32.W))
//  })
//
//  val mem = SyncReadMem(32, SInt(32.W))
//
//  when (io.enable & io.write) {
//    mem.write(io.addr, io.dataIn)
//  }
//  io.dataOut := mem.read(io.addr, io.enable)
//}
//
//object TempMain extends App {
//  Emit(new SyncReadMemTestWrapper(), args)
//}
//
//class TempTest extends AnyFlatSpec with ChiselScalatestTester {
//  behavior of "SyncReadMemTestWrapper"
//
//  it should "mem write first?" in {
//    test(new SyncReadMemTestWrapper) { c =>
//      c.io.enable.poke(true.B)
//      c.io.addr.poke(0.U)
//
//      c.io.dataIn.poke(123.S)
//      c.io.write.poke(true.B)
//      c.clock.step()
//      c.io.write.poke(false.B)
//
//      c.io.dataOut.expect(123.S)
//    }
//  }
//}

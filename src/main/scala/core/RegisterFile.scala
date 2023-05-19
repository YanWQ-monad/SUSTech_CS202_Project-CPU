package core

import util.GenerateOptions
import chisel3._

class RegisterFile(implicit options: GenerateOptions) extends Module {
  val io = IO(new Bundle {
    val rs1Addr = Input(UInt(5.W))
    val rs2Addr = Input(UInt(5.W))
    val rdAddr = Input(UInt(5.W))
    val rdDataIn = Input(SInt(32.W))
    val write = Input(Bool())

    val rs1Out = Output(SInt(32.W))
    val rs2Out = Output(SInt(32.W))
  })

  val debug = if (options.enableDebugPorts) Some(IO(new Bundle {
    val debugAddr = Input(UInt(5.W))
    val debugData = Output(SInt(32.W))
  })) else None

  val mem = Mem(32, SInt(32.W))

  io.rs1Out := Mux(io.rs1Addr === 0.U, 0.S, mem.read(io.rs1Addr))
  io.rs2Out := Mux(io.rs2Addr === 0.U, 0.S, mem.read(io.rs2Addr))
  when (io.write && (io.rdAddr =/= 0.U)) {
    mem.write(io.rdAddr, io.rdDataIn)
  }

  debug.foreach { debug =>
    debug.debugData := mem.read(debug.debugAddr)
  }
}

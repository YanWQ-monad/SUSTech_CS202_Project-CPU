package core

import util._
import board._
import ip._
import chisel3._

class Core(implicit options: GenerateOptions) extends Module {
  val io = IO(new Bundle {
    val cycles = Output(UInt(32.W))
    val external = Flipped(new BoardDataBundle())

    val interrupt = Input(Bool())
    val originPC = Output(UInt(32.W))
  })

  val id = Module(new Decoder())
  val alu = Module(new ALU())
  val cmp = Module(new Comparator())
  val reg = Module(new RegisterFile())
  val mem = Module(new Memory(32768))

  // Core (Sequential) logic
  val cycles = RegInit(0.U(32.W))
  val stage = RegInit(1.B)
  val pc = RegInit(0.U(32.W))
  val pcOld = RegInit(0.U(32.W))
  val originPC = RegInit(0.U(32.W))
  val nextPc = Wire(UInt(32.W))

  when(stage) {
    cycles := cycles + 1.U
  }.otherwise {
    pcOld := pc

    // interrupt handling
    pc := Mux(io.interrupt, 0.U, nextPc)
    when (io.interrupt) { originPC := pc }
  }
  stage := !stage

  // Core (combination) logic
  io.external <> mem.io.external
  io.cycles := cycles
  io.originPC := originPC

  val exeResult = Wire(SInt(32.W))
  mem.io.addrPC := pc
  id.io.instruction := mem.io.dataPC

  val PcSrc = Mux(id.io.pcUseReg, reg.io.rs1Out.asUInt, pc)
  nextPc := Mux((cmp.io.output && id.io.branch) | id.io.jump, PcSrc + id.io.immPc.asUInt, pc + 4.U)

  reg.io.rs1Addr := id.io.rs1
  reg.io.rs2Addr := id.io.rs2
  reg.io.rdAddr := id.io.rd
  reg.io.write := id.io.regWrite && stage
  reg.io.rdDataIn := Mux(id.io.memLoad, mem.io.dataOut, exeResult)

  cmp.io.cmpType := id.io.cmpType
  cmp.io.lhs := reg.io.rs1Out
  cmp.io.rhs := Mux(id.io.useImm, id.io.imm, reg.io.rs2Out)
  alu.io.aluType := id.io.aluType
  alu.io.lhs := Mux(id.io.pcLink, pcOld.asSInt, reg.io.rs1Out)
  alu.io.rhs := reg.io.rs2Out
  alu.io.shamt := Mux(id.io.useImm, id.io.rs2, reg.io.rs2Out(4, 0).asUInt)
  exeResult := Mux(id.io.useALU, alu.io.output, ZeroExt32(cmp.io.output).asSInt)

  mem.io.addr := exeResult.asUInt
  mem.io.memWidth := id.io.memWidth
  mem.io.write := id.io.memWrite && stage  // only perform write on stage=1
  mem.io.willWrite := id.io.memWrite
  mem.io.enable := id.io.memLoad | id.io.memWrite
  mem.io.dataIn := reg.io.rs2Out
  mem.io.unsigned := id.io.memUnsigned
  mem.io.stage := stage
}

package core

import util.{Emit, _}
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class Core extends Module {
  val io = IO(new Bundle {
    val pc = Output(UInt(32.W))
    val ins = Output(UInt(32.W))
    val invalid = Output(Bool())
    val debugRegAddr = Input(UInt(5.W))
    val debugRegData = Output(SInt(32.W))
  })

  val pcLogic = Module(new PCLogic())

  val id = Module(new Decoder())
  val alu = Module(new ALU())
  val cmp = Module(new Comparator())
  val reg = Module(new RegisterFile())
  val mem = Module(new Memory(1024))

  val pc = RegInit(0.U(32.W))
  pc := pcLogic.io.nextPc

  reg.io.debugAddr := io.debugRegAddr
  io.debugRegData := reg.io.debugData
  io.pc := pc
  io.ins := mem.io.dataPC(6, 0)
  io.invalid := id.io.invalid

  val exeResult = Wire(SInt(32.W))
  mem.io.addrPC := pc
  id.io.instruction := mem.io.dataPC

  pcLogic.io.pc := pc
  pcLogic.io.jump := id.io.jump
  pcLogic.io.useReg := id.io.pcUseReg
  pcLogic.io.imm := id.io.immPc
  pcLogic.io.reg := reg.io.rs1Out.asUInt
  pcLogic.io.cond := cmp.io.output & id.io.branch

  reg.io.rs1Addr := id.io.rs1
  reg.io.rs2Addr := id.io.rs2
  reg.io.rdAddr := id.io.rd
  reg.io.write := id.io.regWrite
//  when (id.io.pcLink) { reg.io.rdDataIn := pcLogic.io.nextIns.asSInt }
  when (id.io.memLoad) { reg.io.rdDataIn := mem.io.dataOut }
    .otherwise { reg.io.rdDataIn := exeResult }

  cmp.io.cmpType := id.io.cmpType
  cmp.io.lhs := reg.io.rs1Out
  cmp.io.rhs := reg.io.rs2Out
  alu.io.aluType := id.io.aluType
  alu.io.lhs := Mux(id.io.pcLink, pc.asSInt, reg.io.rs1Out)
  alu.io.rhs := Mux(id.io.useImm, id.io.imm, reg.io.rs2Out)
  alu.io.shamt := id.io.rs2
  exeResult := Mux(id.io.useALU, alu.io.output, ZeroExt32(cmp.io.output).asSInt)

  mem.io.addr := exeResult.asUInt
  mem.io.memWidth := id.io.memWidth
  mem.io.write := id.io.memWrite
  mem.io.enable := id.io.memLoad | id.io.memWrite
  mem.io.dataIn := reg.io.rs2Out
  mem.io.unsigned := id.io.memUnsigned
}

class PCLogic extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val cond = Input(Bool())
    val jump = Input(Bool())
    val useReg = Input(Bool())
    val imm = Input(SInt(32.W))
    val reg = Input(UInt(32.W))

    val nextPc = Output(UInt(32.W))
  })

  val src = Mux(io.useReg, io.reg, io.pc)
  val nextIns = io.pc + 4.U
  io.nextPc := Mux(io.cond | io.jump, src + io.imm.asUInt, nextIns)
}

object VerilogMain extends App {
  implicit val debugMode: Boolean = true
  Emit(new Core(), args)
}

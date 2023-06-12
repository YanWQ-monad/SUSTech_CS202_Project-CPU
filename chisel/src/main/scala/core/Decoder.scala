package core

import util.{switch, _}
import chisel3._
import chisel3.util._

class Decoder extends Module {
  val io = IO(new Bundle {
    val instruction = Input(UInt(32.W))

    val rs1 = Output(UInt(5.W))
    val rs2 = Output(UInt(5.W))
    val rd = Output(UInt(5.W))
    val regWrite = Output(Bool())      // Write Back enable

    val imm = Output(SInt(32.W))       // immediate value for EXE
    val immPc = Output(SInt(32.W))     // immediate value for PC calculation (jumps)

    val branch = Output(Bool())        // conditional jump (BEQ, BLE, ...) enables
    val jump = Output(Bool())          // unconditional jump (JAL, JALR, ...) enables
    val pcUseReg = Output(Bool())      // tell use immediate value or use register value for next PC
    val pcLink = Output(Bool())        // if enabled, EXE's immediate value will be replaced to current PC

    val useImm = Output(Bool())        // whether EXE uses immediate
    val useALU = Output(Bool())        // use ALU or CMP in EXE stage
    val cmpType = Output(CmpType())    // comparing type (LT, GT, EQ, ...)
    val aluType = Output(ALUType())    // ALU type (ADD, SUB, ...)

    val memWrite = Output(Bool())      // memory write
    val memLoad = Output(Bool())       // memory load
    val memWidth = Output(MemWidth())  // read/write data width (1/2/4 bytes)
    val memUnsigned = Output(Bool())   // is read unsigned value? (LBU, LHU)

    val invalid = Output(Bool())       // the instruction is invalid
  })

  val ins = io.instruction  // short name

  val funct7 = ins(31, 25)
  val rs2 = ins(24, 20)
  val rs1 = ins(19, 15)
  val funct3 = ins(14, 12)
  val rd = ins(11, 7)
  val opcode = ins(6, 0)

  io.rs2 := rs2
  io.rd := rd

  val shamt = rs2

  val sign = ins(31)

  val immI = ins(31, 20).asSInt
  val immS = Cat(ins(31, 25), ins(11, 7)).asSInt
  val immB = Cat(ins(31), ins(7), ins(30, 25), ins(11, 8), 0.U(1.W)).asSInt
  val immU = Cat(ins(31, 12), 0.U(12.W)).asSInt
  val immJ = Cat(ins(31), ins(19, 12), ins(20), ins(30, 21), 0.U(1.W)).asSInt
  val imm4 = 4.S(32.W)

  def set(rs1: UInt, imm: Data, immPc: Data, branch: Bool, jump: Bool, pcUseReg: Data, pcLink: Data, useImm: Data, useALU: Data, cmpType: Data, aluType: Data, memWrite: Bool, memLoad: Bool, memWidth: Data, memUnsigned: Data, regWrite: Bool) = {
    io.rs1 := rs1
    io.imm := imm
    io.immPc := immPc
    io.branch := branch
    io.jump := jump
    io.pcUseReg := pcUseReg
    io.pcLink := pcLink
    io.useImm := useImm
    io.useALU := useALU
    io.cmpType := cmpType
    io.aluType := aluType
    io.memWrite := memWrite
    io.memLoad := memLoad
    io.memWidth := memWidth
    io.memUnsigned := memUnsigned
    io.regWrite := regWrite
    io.invalid := false.B
  }

  val X = DontCare  // short name for "don't care"

  set(rs1, X, X, 0.B, 0.B, X, 0.B, X, X, X, X, 0.B, 0.B, X, X, 0.B)  // default
  io.invalid := true.B

  /* rs1, imm, immPC, B, J, useReg, link, useImm, useALU, cmpType, aluType, memWrite, memLoad, regWrite */
  switch (opcode)
    .is ("b0110111".U) {  // LUI
      set(0.U, immU, X, 0.B, 0.B, X, 0.B, 1.B, 1.B, X, ALUType.Add, 0.B, 0.B, X, X, 1.B)
    }
    .is ("b0010111".U) {  // AUIPC
      set(rs1, immU, X, 0.B, 0.B, 0.B, 1.B, 1.B, 1.B, X, ALUType.Add, 0.B, 0.B, X, X, 1.B)
    }
    .is ("b1101111".U) {  // JAL
      set(rs1, imm4, immJ, 0.B, 1.B, 0.B, 1.B, 1.B, 1.B, X, ALUType.Add, 0.B, 0.B, X, X, 1.B)
    }
    .is ("b1100111".U) {  // JALR
      set(rs1, imm4, immI, 0.B, 1.B, 1.B, 1.B, 1.B, 1.B, X, ALUType.Add, 0.B, 0.B, X, X, 1.B)
    }
    .is ("b1100011".U) {  // branches
      def setBranch(cmpType: Data): Unit =
        set(rs1, X, immB, 1.B, 0.B, 0.B, 0.B, 0.B, X, cmpType, X, 0.B, 0.B, X, X, 0.B)
      switch (funct3)
        .is ("b000".U) { setBranch(CmpType.Eq ) }  // BEQ
        .is ("b001".U) { setBranch(CmpType.Ne ) }  // BNE
        .is ("b100".U) { setBranch(CmpType.Lt ) }  // BLT
        .is ("b101".U) { setBranch(CmpType.Ge ) }  // BGE
        .is ("b110".U) { setBranch(CmpType.LtU) }  // BLTU
        .is ("b111".U) { setBranch(CmpType.GeU) }  // BGEU
    }
    .is ("b0000011".U) {  // loads
      def setLoads(width: MemWidth.Type, unsigned: Data): Unit =
        set(rs1, immI, X, 0.B, 0.B, X, 0.B, 1.B, 1.B, X, ALUType.Add, 0.B, 1.B, width, unsigned, 1.B)

      switch (funct3)
        .is ("b000".U) { setLoads(MemWidth.Byte, 0.B) }
        .is ("b001".U) { setLoads(MemWidth.Half, 0.B) }
        .is ("b010".U) { setLoads(MemWidth.Word, X) }
        .is ("b100".U) { setLoads(MemWidth.Byte, 1.B) }
        .is ("b101".U) { setLoads(MemWidth.Half, 1.B) }
    }
    .is ("b0100011".U) {  // stores
      def setLoads(width: MemWidth.Type): Unit =
        set(rs1, immS, X, 0.B, 0.B, X, 0.B, 1.B, 1.B, X, ALUType.Add, 1.B, 0.B, width, X, 0.B)

      switch (funct3)
        .is ("b000".U) { setLoads(MemWidth.Byte) }
        .is ("b001".U) { setLoads(MemWidth.Half) }
        .is ("b010".U) { setLoads(MemWidth.Word) }
    }
    .is ("b0010011".U) {  // op-imm
      def setOpImm(useALU: Bool, cmpType: Data, aluType: Data): Unit =
        set(rs1, immI, X, 0.B, 0.B, X, 0.B, 1.B, useALU, cmpType, aluType, 0.B, 0.B, X, X, 1.B)

      switch (funct3)
        .is ("b000".U) { setOpImm(1.B, X, ALUType.Add) }  // ADDI
        .is ("b010".U) { setOpImm(0.B, CmpType.Lt, X) }  // SLTI
        .is ("b011".U) { setOpImm(0.B, CmpType.LtU, X) }  // SLTIU
        .is ("b100".U) { setOpImm(1.B, X, ALUType.Xor) }  // XORI
        .is ("b110".U) { setOpImm(1.B, X, ALUType.Or) }  // ORI
        .is ("b111".U) { setOpImm(1.B, X, ALUType.And) }  // ANDI
        .is ("b001".U) { setOpImm(1.B, X, ALUType.Sll) }  // SLLI
        .is ("b101".U) {
          switch (funct7(5))
            .is (0.B) { setOpImm(1.B, X, ALUType.Srl) }  // SRLI
            .default  { setOpImm(1.B, X, ALUType.Sra) }  // SRAI
        }
    }
    .is ("b0110011".U) {  // op
      /* rs1, imm, B, J, useReg, link, useImm, useALU, cmpType, aluType, memWrite, memLoad, regWrite */
      def setOp(useALU: Bool, cmpType: Data, aluType: Data): Unit =
        set(rs1, X, X, 0.B, 0.B, X, 0.B, 0.B, useALU, cmpType, aluType, 0.B, 0.B, X, X, 1.B)

      switch (funct3)
        .is ("b000".U) {
          switch (funct7(5))
            .is (0.B) { setOp(1.B, X, ALUType.Add) }  // ADD
            .default  { setOp(1.B, X, ALUType.Sub) }  // SUB
        }
        .is ("b001".U) { setOp(1.B, X, ALUType.Sll) }  // SLL
        .is ("b010".U) { setOp(0.B, CmpType.Lt, X) }  // SLT
        .is ("b011".U) { setOp(0.B, CmpType.LtU, X) }  // SLTU
        .is ("b100".U) { setOp(1.B, X, ALUType.Xor) }  // XOR
        .is ("b101".U) {
          switch (funct7(5))
            .is (0.B) { setOp(1.B, X, ALUType.Srl) }  // SRL
            .default  { setOp(1.B, X, ALUType.Sra) }  // SRA
        }
        .is ("b110".U) { setOp(1.B, X, ALUType.Or) }  // OR
        .is ("b111".U) { setOp(1.B, X, ALUType.And) }  // AND
    }
    // .is ("b0001111".U) {}  // FENCE
    // .is ("b1110011".U) {}  // ECALL & EBREAK
    .default {}
}

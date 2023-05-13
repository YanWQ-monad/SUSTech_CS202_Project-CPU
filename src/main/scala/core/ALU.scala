package core

import util._
import chisel3._

class ALU extends Module {
  val io = IO(new Bundle {
    val lhs = Input(SInt(32.W))
    val rhs = Input(SInt(32.W))
    val shamt = Input(UInt(5.W))
    val aluType = Input(ALUType())

    val output = Output(SInt(32.W))
  })

  switch (io.aluType)
    .is (ALUType.Add) { io.output := io.lhs + io.rhs }
    .is (ALUType.Sub) { io.output := io.lhs - io.rhs }
    .is (ALUType.Xor) { io.output := io.lhs ^ io.rhs }
    .is (ALUType.Or)  { io.output := io.lhs | io.rhs }
    .is (ALUType.And) { io.output := io.lhs & io.rhs }
    .is (ALUType.Sll) { io.output := io.lhs << io.shamt }
    .is (ALUType.Srl) { io.output := (io.lhs.asUInt >> io.shamt).asSInt }
    .is (ALUType.Sra) { io.output := io.lhs >> io.shamt }
    .default { io.output := DontCare }
}

object ALUType extends ChiselEnum {
  val Add, Sub, Xor, Or, And, Sll, Srl, Sra = Value
}

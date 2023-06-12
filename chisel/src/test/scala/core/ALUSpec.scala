package core

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ALUSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "core.ALU"

  it should "yield correct result in ADD" in {
    test(new ALU) { dut =>
      dut.io.aluType.poke(ALUType.Add)
      dut.io.lhs.poke(15.S)
      dut.io.rhs.poke(16.S)
      dut.io.output.expect(31.S)

      dut.io.lhs.poke(-1.S)
      dut.io.rhs.poke(1.S)
      dut.io.output.expect(0.S)
    }
  }

  it should "yield correct result in shifts" in {
    test(new ALU) { dut =>
      dut.io.aluType.poke(ALUType.Sll)
      dut.io.lhs.poke(15.S)
      dut.io.shamt.poke(3.U)
      dut.io.output.expect((15 << 3).S)

      dut.io.aluType.poke(ALUType.Srl)
      dut.io.lhs.poke(-1.S)  // 0xFFFFFFFF
      dut.io.shamt.poke(16.U)
      dut.io.output.expect(0xFFFF.S)

      dut.io.aluType.poke(ALUType.Sra)
      dut.io.lhs.poke(-32.S)
      dut.io.shamt.poke(4.U)
      dut.io.output.expect((-(32 >> 4)).S)
    }
  }
}

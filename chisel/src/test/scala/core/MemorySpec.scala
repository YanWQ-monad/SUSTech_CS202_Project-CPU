package core

import util.GenerateOptions
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MemorySpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "core.Memory"

  def write(addr: UInt, data: SInt, width: MemWidth.Type)(implicit dut: Memory): Unit = {
    dut.io.addr.poke(addr)
    dut.io.memWidth.poke(width)
    dut.io.write.poke(true.B)
    dut.io.dataIn.poke(data)

    dut.clock.step(1)
    dut.io.write.poke(false.B)
  }

  def read(addr: UInt, width: MemWidth.Type, unsigned: Bool)(implicit dut: Memory): SInt = {
    dut.io.addr.poke(addr)
    dut.io.memWidth.poke(width)
    dut.io.unsigned.poke(unsigned)

    dut.clock.step(1)
    dut.io.dataOut
  }

  def getMemory: Memory = {
    implicit val options = new GenerateOptions(
      true,
      false,
      100_000_000,
      10_000_000,
      8_500_000,
      40_000_000)
    new Memory(32)
  }

  it should "read and write word" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)
      write(4.U, 0x01020304.S, MemWidth.Word)
      read(4.U, MemWidth.Word, true.B).expect(0x01020304.S)
    }
  }

  it should "read and write half-word" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)
      write(4.U, 0x0102.S, MemWidth.Half)
      read(4.U, MemWidth.Half, true.B).expect(0x0102.S)

      write(6.U, 0x0102.S, MemWidth.Half)
      read(6.U, MemWidth.Half, true.B).expect(0x0102.S)
    }
  }

  it should "read and write byte" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      for (i <- 0 until 8) {
        write(i.U, i.S, MemWidth.Half)
        read(i.U, MemWidth.Half, true.B).expect(i.S)
      }
    }
  }

  it should "read little endian in half-word" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      write(4.U, 0x01020304.S, MemWidth.Word)
      read(4.U, MemWidth.Half, true.B).expect(0x0304.S)
      read(6.U, MemWidth.Half, true.B).expect(0x0102.S)
    }
  }

  it should "write little endian in half-word" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      write(4.U, 0x0304.S, MemWidth.Half)
      write(6.U, 0x0102.S, MemWidth.Half)
      read(4.U, MemWidth.Word, true.B).expect(0x01020304.S)
    }
  }

  it should "read little endian in byte" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      write(4.U, 0x01020304.S, MemWidth.Word)
      read(4.U, MemWidth.Byte, true.B).expect(0x04.S)
      read(5.U, MemWidth.Byte, true.B).expect(0x03.S)
      read(6.U, MemWidth.Byte, true.B).expect(0x02.S)
      read(7.U, MemWidth.Byte, true.B).expect(0x01.S)
    }
  }

  it should "write little endian in byte" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      write(4.U, 0x04.S, MemWidth.Byte)
      write(5.U, 0x03.S, MemWidth.Byte)
      write(6.U, 0x02.S, MemWidth.Byte)
      write(7.U, 0x01.S, MemWidth.Byte)
      read(4.U, MemWidth.Word, true.B).expect(0x01020304.S)
    }
  }

  it should "partial write of half-word" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      write(4.U, 0x01020304.S, MemWidth.Word)
      write(6.U, 0x0506.S, MemWidth.Half)
      read(4.U, MemWidth.Word, true.B).expect(0x05060304.S)
      write(4.U, 0x0708.S, MemWidth.Half)
      read(4.U, MemWidth.Word, true.B).expect(0x05060708.S)
    }
  }

  it should "partial write of byte" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      write(4.U, 0x01020304.S, MemWidth.Word)
      write(6.U, 0x05.S, MemWidth.Byte)
      read(4.U, MemWidth.Word, true.B).expect(0x01050304.S)
      write(7.U, 0x06.S, MemWidth.Byte)
      read(4.U, MemWidth.Word, true.B).expect(0x06050304.S)
      write(4.U, 0x07.S, MemWidth.Byte)
      read(4.U, MemWidth.Word, true.B).expect(0x06050307.S)
      write(5.U, 0x08.S, MemWidth.Byte)
      read(4.U, MemWidth.Word, true.B).expect(0x06050807.S)
    }
  }

  it should "read signed half-word" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      write(4.U, 0xF000F000.S, MemWidth.Word)
      read(6.U, MemWidth.Half, false.B).expect(0xFFFFF000.S)
      read(4.U, MemWidth.Half, false.B).expect(0xFFFFF000.S)
    }
  }

  it should "read signed byte" in {
    test(getMemory) { implicit dut =>
      dut.io.enable.poke(true.B)

      write(4.U, 0xF0F0F0F0.S, MemWidth.Word)
      read(4.U, MemWidth.Byte, false.B).expect(0xFFFFFFF0.S)
      read(5.U, MemWidth.Byte, false.B).expect(0xFFFFFFF0.S)
      read(6.U, MemWidth.Byte, false.B).expect(0xFFFFFFF0.S)
      read(7.U, MemWidth.Byte, false.B).expect(0xFFFFFFF0.S)
    }
  }
}

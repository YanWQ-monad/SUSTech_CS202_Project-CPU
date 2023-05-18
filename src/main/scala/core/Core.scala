package core

import util._
import misc._
import ip._
import chisel3._
import chisel3.util._

class Core(implicit options: GenerateOptions) extends Module {
  val io = IO(new RealBoardDataBundle())

  val debug = if (options.enableDebugPorts) Some(IO(new Bundle {
    val pc = Output(UInt(32.W))
    val invalid = Output(Bool())
    val debugRegAddr = Input(UInt(5.W))
    val debugRegData = Output(SInt(32.W))
  })) else None

  val clocks = Module(new ClockWizard())

  withClockAndReset(clocks.io.clockStd, clocks.reset) {
    val pcLogic = Module(new PCLogic())

    val id = Module(new Decoder())
    val alu = Module(new ALU())
    val cmp = Module(new Comparator())
    val reg = Module(new RegisterFile())
    val mem = Module(new Memory(32768))

    val uart = Module(new UARTWrap(8))

    // Core (Sequential) logic
    val cycles = RegInit(0.U(32.W))
    val step = RegInit(1.B)
    val pc = RegInit(0.U(32.W))
    val pcOld = RegInit(0.U(32.W))

    when(step) {
      cycles := cycles + 1.U
    }.otherwise {
      pcOld := pc
      pc := pcLogic.io.nextPc
    }
    step := !step

    // Core (combination) logic
    mem.io.externalIn := DontCare
    mem.io.externalIn(0) := cycles
    //  mem.io.externalIn(1) := io.extIn.asUInt
    //  io.extOut := mem.io.externalOut(0)

    val exeResult = Wire(SInt(32.W))
    mem.io.addrPC := pc
    id.io.instruction := mem.io.dataPC

    pcLogic.io.pc := pc
    pcLogic.io.jump := id.io.jump
    pcLogic.io.useReg := id.io.pcUseReg
    pcLogic.io.imm := id.io.immPc
    pcLogic.io.reg := reg.io.rs1Out.asUInt
    pcLogic.io.cond := cmp.io.output && id.io.branch

    reg.io.rs1Addr := id.io.rs1
    reg.io.rs2Addr := id.io.rs2
    reg.io.rdAddr := id.io.rd
    reg.io.write := id.io.regWrite && step
    when(id.io.memLoad) {
      reg.io.rdDataIn := mem.io.dataOut
    }
      .otherwise {
        reg.io.rdDataIn := exeResult
      }

    cmp.io.cmpType := id.io.cmpType
    cmp.io.lhs := reg.io.rs1Out
    cmp.io.rhs := reg.io.rs2Out
    alu.io.aluType := id.io.aluType
    alu.io.lhs := Mux(id.io.pcLink, pcOld.asSInt, reg.io.rs1Out)
    alu.io.rhs := Mux(id.io.useImm, id.io.imm, reg.io.rs2Out)
    alu.io.shamt := Mux(id.io.useImm, id.io.rs2, reg.io.rs2Out(4, 0).asUInt)
    exeResult := Mux(id.io.useALU, alu.io.output, ZeroExt32(cmp.io.output).asSInt)

    mem.io.addr := exeResult.asUInt
    mem.io.memWidth := id.io.memWidth
    mem.io.write := id.io.memWrite && step
    mem.io.willWrite := id.io.memWrite
    mem.io.enable := id.io.memLoad | id.io.memWrite
    mem.io.dataIn := reg.io.rs2Out
    mem.io.unsigned := id.io.memUnsigned
    mem.io.step := step
    uart.io.dataIn <> mem.io.uartOut
    uart.io.dataOut <> mem.io.uartIn

    uart.io.rx := io.uartRx
    io.uartTx := uart.io.tx
    uart.io.uartClock := clocks.io.clockUart

    // Debug ports
    debug.foreach { debug =>
      reg.debug.get.debugAddr := debug.debugRegAddr
      debug.debugRegData := reg.debug.get.debugData
      debug.pc := pc
      debug.invalid := id.io.invalid
    }
  }
}

class UARTWrap(dataBits: Int) extends Module {
  val io = IO(new Bundle {
    val tx = Output(Bool())
    val rx = Input(Bool())

    val dataIn = Flipped(Decoupled(UInt(dataBits.W)))
    val dataOut = Decoupled(UInt(dataBits.W))

    val uartClock = Input(Clock())
  })

  val txQueue = Module(new CrossClockQueue(UInt(dataBits.W), 64))  // CPU  --> UART
  val rxQueue = Module(new CrossClockQueue(UInt(dataBits.W), 64))  // UART --> CPU

  val uart = withClock(io.uartClock) { Module(new UART(dataBits)) }

  uart.io.rx := io.rx
  io.tx := uart.io.tx

  txQueue.io.clkEnq := clock
  txQueue.io.clkDeq := io.uartClock
  txQueue.io.enq <> io.dataIn
  txQueue.io.deq <> uart.io.dataIn

  rxQueue.io.clkEnq := io.uartClock
  rxQueue.io.clkDeq := clock
  rxQueue.io.enq <> uart.io.dataOut
  rxQueue.io.deq <> io.dataOut
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
  implicit val options = new GenerateOptions(
    false,
    true,
    100_000_000,
    10_000_000,
    8_500_000,
    40_000_000)

  Emit(new Core(), args)
}

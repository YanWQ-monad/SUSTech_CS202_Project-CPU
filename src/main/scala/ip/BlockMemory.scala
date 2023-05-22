package ip

import util.GenerateOptions
import chisel3._

private class RawBlockMemory(id: Int, addrWidth: Int, dataWidth: Int) extends BlackBox {
  val io = IO(new Bundle {
    val addra = Input(UInt(addrWidth.W))
    val clka = Input(Clock())
    val dina = Input(UInt(dataWidth.W))
    val douta = Output(UInt(dataWidth.W))
    val wea = Input(Bool())

    val addrb = Input(UInt(addrWidth.W))
    val clkb = Input(Clock())
    val dinb = Input(UInt(dataWidth.W))
    val doutb = Output(UInt(dataWidth.W))
    val web = Input(Bool())
  })

  override def desiredName = f"blk_mem_gen_${id}%d"
}

class MemoryPort(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val addr = Input(UInt(addrWidth.W))
  val dataIn = Input(UInt(dataWidth.W))
  val dataOut = Output(UInt(dataWidth.W))
  val write = Input(Bool())

  def init(): Unit = {
    addr := DontCare
    dataIn := DontCare
    write := false.B
  }

  def setRead(addr: UInt): UInt = {
    this.addr := addr
    dataOut
  }

  def setWrite(addr: UInt, data: UInt, write: Bool): Unit = {
    this.addr := addr
    this.dataIn := data
    this.write := write
  }
}

// BlockMemory that calls the Vivado IP (if IP is enabled in `options`),
// or otherwise use array as memory at out-of-Vivado simulation
class BlockMemory(id: Int, depth: BigInt, val dataWidth: Int, simInitializeFile: Option[String] = None)(implicit options: GenerateOptions) extends Module {
  val addrWidth = LazyList.from(1).filter(x => (1 << x) >= depth).head
  val io1 = IO(new MemoryPort(addrWidth, dataWidth))
  val io2 = IO(new MemoryPort(addrWidth, dataWidth))

  val io = IO(new Bundle {
    val clockB = Input(Clock())
  })

  if (options.useIP) {
    val raw = Module(new RawBlockMemory(id, addrWidth, dataWidth))
    raw.io.clka := clock
    raw.io.addra := io1.addr
    raw.io.dina := io1.dataIn
    io1.dataOut := raw.io.douta
    raw.io.wea := io1.write

    raw.io.clkb := io.clockB
    raw.io.addrb := io2.addr
    raw.io.dinb := io2.dataIn
    io2.dataOut := raw.io.doutb
    raw.io.web := io2.write
  }
  else {
    val mem = SyncReadMem(1 << addrWidth, UInt(dataWidth.W))
    simInitializeFile.foreach { file =>
      chisel3.util.experimental.loadMemoryFromFileInline(mem, file)
    }

    def generate_for(io: MemoryPort, clock: Clock) = {
      io.dataOut := mem.read(io.addr, clock)
      when (io.write) {
        mem.write(io.addr, io.dataIn, clock)
      }
    }

    generate_for(io1, clock)
    generate_for(io2, io.clockB)
  }
}

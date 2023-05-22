package ip

import util.GenerateOptions
import chisel3._

private class RawBlockMemoryRom(id: Int, addrWidth: Int, dataWidth: Int) extends BlackBox {
  val io = IO(new Bundle {
    val addra = Input(UInt(addrWidth.W))
    val clka = Input(Clock())
    val douta = Output(UInt(dataWidth.W))
  })

  override def desiredName = f"blk_mem_gen_${id}%d"
}

class MemoryRomPort(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val addr = Input(UInt(addrWidth.W))
  val dataOut = Output(UInt(dataWidth.W))

  def init(): Unit = {
    addr := DontCare
  }

  def setRead(addr: UInt): UInt = {
    this.addr := addr
    dataOut
  }
}

class BlockMemoryRom(id: Int, depth: BigInt, val dataWidth: Int, simInitializeFile: Option[String] = None)(implicit options: GenerateOptions) extends Module {
  val addrWidth = LazyList.from(1).filter(x => (1 << x) >= depth).head
  val io = IO(new MemoryRomPort(addrWidth, dataWidth))

  if (options.useIP) {
    val raw = Module(new RawBlockMemoryRom(id, addrWidth, dataWidth))
    raw.io.clka := clock
    raw.io.addra := io.addr
    io.dataOut := raw.io.douta
  }
  else {
    val mem = SyncReadMem(1 << addrWidth, UInt(dataWidth.W))
    simInitializeFile.foreach { file =>
      chisel3.util.experimental.loadMemoryFromFileInline(mem, file)
    }

    io.dataOut := mem.read(io.addr)
  }
}

package util

import chisel3._
import circt.stage.ChiselStage

object Emit {
  def apply(gen: => RawModule, args: Array[String]): Unit = {
    val emitArgs = if (args.isEmpty) {
      Array("--target-dir", "verilog")
    } else {
      args
    }

    ChiselStage.emitSystemVerilogFile(
      gen,
      emitArgs,
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
  }
}

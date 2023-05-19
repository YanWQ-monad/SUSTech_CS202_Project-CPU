package misc

import util.Helper
import chisel3._
import chisel3.util._

class CrossClockQueue[T <: Data](gen: T, depth: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(gen))
    val deq = Decoupled(gen)

    val clkEnq = Input(Clock())
    val clkDeq = Input(Clock())
  })

  // data: [ _ 1 2 3 4 _ _ _ ]
  //           |       |
  //          deq     enq --->

  val mem = withClock(io.clkEnq) {
    Mem(depth, gen)
  }

  val depthBits = Helper.log2(depth)
  val enqIdx = withClock(io.clkEnq) {
    RegInit(0.U(depthBits.W))
  }
  val deqIdx = withClock(io.clkDeq) {
    RegInit(0.U(depthBits.W))
  }

  val enqNextIdx = enqIdx + 1.U
  val deqNextIdx = deqIdx + 1.U

  val full = enqNextIdx === deqIdx
  val empty = enqIdx === deqIdx

  io.enq.ready := !full
  withClock(io.clkEnq) {
    when(!full && io.enq.valid) {
      mem.write(enqIdx, io.enq.bits)
      enqIdx := enqNextIdx
    }
  }

  io.deq.valid := !empty
  io.deq.bits := mem.read(deqIdx, io.clkDeq)
  when(!empty && io.deq.ready) {
    deqIdx := deqNextIdx
  }
}

//object TempMain extends App {
//  Emit(new CrossClockQueue(UInt(8.W), 16), args)
//}

package core

import util.{Emit, switch}
import chisel3._

class Comparator extends Module {
  val io = IO(new Bundle {
    val cmpType = Input(CmpType())

    val lhs = Input(SInt(32.W))
    val rhs = Input(SInt(32.W))

    val output = Output(Bool())
  })

  val impl = Module(new ComparatorImpl)
  impl.io.lhs := io.lhs
  impl.io.rhs := io.rhs
  io.output := impl.io.output

  def assignToImpl(equals: => Data, invert: => Data, unsigned: => Data): Unit = {
    impl.io.equals := equals
    impl.io.invert := invert
    impl.io.unsigned := unsigned
  }

  switch (io.cmpType)
    .is(CmpType.Eq ) { assignToImpl(1.B, 0.B, DontCare) }
    .is(CmpType.Ne ) { assignToImpl(1.B, 1.B, DontCare) }
    .is(CmpType.Lt ) { assignToImpl(0.B, 0.B, 0.B) }
    .is(CmpType.Ge ) { assignToImpl(0.B, 1.B, 0.B) }
    .is(CmpType.LtU) { assignToImpl(0.B, 0.B, 1.B) }
    .is(CmpType.GeU) { assignToImpl(0.B, 1.B, 1.B) }
    .default { assignToImpl(DontCare, DontCare, DontCare) }
}

object CmpType extends ChiselEnum {
  val Eq, Ne, Lt, Ge, LtU, GeU = Value
}

class ComparatorImpl extends Module {
  val io = IO(new Bundle {
    val equals = Input(Bool())
    val invert = Input(Bool())
    val unsigned = Input(Bool())

    val lhs = Input(SInt(32.W))
    val rhs = Input(SInt(32.W))

    val output = Output(Bool())
  })

  val equals = io.lhs === io.rhs
  val lessThanS = io.lhs < io.rhs
  val lessThanU = io.lhs.asUInt < io.rhs.asUInt

  val interim = Wire(Bool())
  when (io.equals === true.B) {
    interim := equals
  } .elsewhen (io.unsigned === true.B) {
    interim := lessThanU
  } .otherwise {
    interim := lessThanS
  }

  io.output := interim ^ io.invert
}

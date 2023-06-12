package util

import chisel3._
import chisel3.experimental.SourceInfo

object switch {
  def apply[T <: Element](cond: => T): SwitchContext[T] = new SwitchContext(cond, None, Set.empty)
}

class SwitchContext[T <: Element](cond: T, whenContext: Option[WhenContext], lits: Set[BigInt]) {
  def is(v: Iterable[T])(block: => Any)(
    implicit sourceInfo: SourceInfo,
    compileOptions:      CompileOptions
  ): SwitchContext[T] = {
    val newLits = v.map { w =>
      require(w.litOption.isDefined, "is condition must be literal")
      val value = w.litValue
      require(!lits.contains(value), "all is conditions must be mutually exclusive!")
      value
    }

    def p = v.map(_.asUInt === cond.asUInt).reduce(_ || _)
    whenContext match {
      case Some(w) => new SwitchContext(cond, Some(w.elsewhen(p)(block)), lits ++ newLits)
      case None => new SwitchContext(cond, Some(when(p)(block)), lits ++ newLits)
    }
  }

  def is(vr: T*)(block: => Any)(
    implicit sourceInfo: SourceInfo,
    compileOptions:      CompileOptions
  ): SwitchContext[T] = is(vr.toList)(block)

  def default(block: => Any)(
    implicit sourceInfo: SourceInfo,
    compileOptions:      CompileOptions
  ): Unit = {
    whenContext match {
      case Some(w) => w.otherwise(block)
      case None => require(false, "Do not use single 'default'")
    }
  }
}

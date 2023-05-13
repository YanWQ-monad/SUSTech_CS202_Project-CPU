package util

import chisel3._
import chisel3.experimental.SourceInfo

object switch {
  def apply[T <: Element](
    cond:  => T
  )(
    implicit sourceInfo: SourceInfo,
    compileOptions:      CompileOptions
  ): SwitchContext[T] = {
    new SwitchContext(cond, None, Set.empty)
  }
}

/**  A WhenContext may represent a when, and elsewhen, or an
  *  otherwise. Since FIRRTL does not have an "elsif" statement,
  *  alternatives must be mapped to nested if-else statements inside
  *  the alternatives of the preceeding condition. In order to emit
  *  proper FIRRTL, it is necessary to keep track of the depth of
  *  nesting of the FIRRTL whens. Due to the "thin frontend" nature of
  *  Chisel3, it is not possible to know if a when or elsewhen has a
  *  succeeding elsewhen or otherwise; therefore, this information is
  *  added by preprocessing the command queue.
  */
class SwitchContext[T <: Element](cond: T, whenContext: Option[WhenContext], lits: Set[BigInt]) {
  def is(
    v:     Iterable[T]
  )(block: => Any
  )(
    implicit sourceInfo: SourceInfo,
    compileOptions:      CompileOptions
  ): SwitchContext[T] = {
    if (v.nonEmpty) {
      val newLits = v.map { w =>
        require(w.litOption.isDefined, "is condition must be literal")
        val value = w.litValue
        require(!lits.contains(value), "all is conditions must be mutually exclusive!")
        value
      }

      // def instead of val so that logic ends up in legal place
      def p = v.map(_.asUInt === cond.asUInt).reduce(_ || _)
      whenContext match {
        case Some(w) => new SwitchContext(cond, Some(w.elsewhen(p)(block)), lits ++ newLits)
        case None    => new SwitchContext(cond, Some(when(p)(block)), lits ++ newLits)
      }
    } else {
      this
    }
  }

  def is(v: T)(block: => Any)(implicit sourceInfo: SourceInfo, compileOptions: CompileOptions): SwitchContext[T] =
    is(Seq(v))(block)

  def is(
    v:     T,
    vr:    T*
  )(block: => Any
  )(
    implicit sourceInfo: SourceInfo,
    compileOptions:      CompileOptions
  ): SwitchContext[T] = is(v :: vr.toList)(block)

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

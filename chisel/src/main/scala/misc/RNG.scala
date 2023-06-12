package misc

import chisel3._
import chisel3.util.random._

object RNG {
  private val primes: LazyList[Int] = 2 #:: LazyList.from(3).filter { n => !primes.takeWhile(_ <= math.sqrt(n)).exists(n % _ == 0) }

  def apply(width: Int): UInt = {
    val groupLength = 8
    RegNext(VecInit(
      primes
        .slice(6, width / groupLength + 6)
        .zip(Seq(1017L, 393241L, 6291469L, 402653189L))
        .map(p => GaloisLFSR.maxPeriod(p._1, true.B, Some(p._2))(groupLength - 1, 0)))
      .asUInt)
  }
}

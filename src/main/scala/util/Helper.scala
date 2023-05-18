package util

object Helper {
  def log2(n: Int): Int = {
    LazyList.from(1).filter(x => n <= (1 << x)).head
  }
}

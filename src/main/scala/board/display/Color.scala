package board.display

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor

class RGB4BitBundle extends Bundle {
  val r = UInt(4.W)
  val g = UInt(4.W)
  val b = UInt(4.W)
}

object RGB4BitBundle {
  def fromRGBLiteral(r: Int, g: Int, b: Int): RGB4BitBundle =
    (new RGB4BitBundle).Lit(_.r -> r.U(4.W), _.g -> g.U(4.W), _.b -> b.U(4.W))
  def Black: RGB4BitBundle = fromRGBLiteral(0, 0, 0)
}

class ColorRGB4Bit(r_ : Int, g_ : Int, b_ : Int) {
  private def makeBetween(value: Int, min: Int, max: Int): Int =
    Math.max(min, Math.min(max, value))

  val r: Int = makeBetween(r_ >> 4, 0, 0xF)
  val g: Int = makeBetween(g_ >> 4, 0, 0xF)
  val b: Int = makeBetween(b_ >> 4, 0, 0xF)

  def asChiselBundle: RGB4BitBundle = RGB4BitBundle.fromRGBLiteral(r, g, b)
}

object DraculaPalette {
  val Background = new ColorRGB4Bit(40, 42, 54)
  val CurrentLine = new ColorRGB4Bit(68, 71, 90)
  val Foreground = new ColorRGB4Bit(248, 248, 242)
  val Comment = new ColorRGB4Bit(98, 114, 164)
  val Cyan = new ColorRGB4Bit(139, 233, 253)
  val Green = new ColorRGB4Bit(80, 250, 123)
  val Orange = new ColorRGB4Bit(255, 184, 108)
  val Pink = new ColorRGB4Bit(255, 121, 198)
  val Purple = new ColorRGB4Bit(189, 147, 249)
  val Red = new ColorRGB4Bit(255, 85, 85)
  val Yellow = new ColorRGB4Bit(241, 250, 140)
}

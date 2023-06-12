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

class ColorRGB4Bit(val r : Int, val g : Int, val b : Int) {
  def asChiselBundle: RGB4BitBundle = RGB4BitBundle.fromRGBLiteral(r, g, b)
}

object ColorRGB4Bit {
  private def makeBetween(value: Int, min: Int, max: Int): Int =
    Math.max(min, Math.min(max, value))

  def fromFullRGB(r: Int, g: Int, b: Int): ColorRGB4Bit = new ColorRGB4Bit(
    makeBetween((r >> 4) + ((r >> 3) & 1), 0, 0xF),
    makeBetween((g >> 4) + ((r >> 3) & 1), 0, 0xF),
    makeBetween((b >> 4) + ((r >> 3) & 1), 0, 0xF),
  )
}

object DraculaPalette {
  val Background = ColorRGB4Bit.fromFullRGB(40, 42, 54)
  val CurrentLine = ColorRGB4Bit.fromFullRGB(68, 71, 90)
  val Foreground = ColorRGB4Bit.fromFullRGB(248, 248, 242)
  val Comment = ColorRGB4Bit.fromFullRGB(98, 114, 164)
  val Cyan = ColorRGB4Bit.fromFullRGB(139, 233, 253)
  val Green = ColorRGB4Bit.fromFullRGB(80, 250, 123)
  val Orange = ColorRGB4Bit.fromFullRGB(255, 184, 108)
  val Pink = ColorRGB4Bit.fromFullRGB(255, 121, 198)
  val Purple = ColorRGB4Bit.fromFullRGB(189, 147, 249)
  val Red = ColorRGB4Bit.fromFullRGB(255, 85, 85)
  val Yellow = ColorRGB4Bit.fromFullRGB(241, 250, 140)
}

object DraculaPaletteModified {
  val Background = new ColorRGB4Bit(0x2, 0x2, 0x2)
//  val Background = ColorRGB4Bit.fromFullRGB(40, 42, 54)
  val CurrentLine = ColorRGB4Bit.fromFullRGB(68, 71, 90)
  val Foreground = ColorRGB4Bit.fromFullRGB(248, 248, 242)
  val Comment = ColorRGB4Bit.fromFullRGB(98, 114, 164)
  val Black = new ColorRGB4Bit(0x2, 0x2, 0x2)
  val BrightBlack = new ColorRGB4Bit(0x6, 0x7, 0xA)
  val Red = new ColorRGB4Bit(0xF, 0x5, 0x5)
  val BrightRed = new ColorRGB4Bit(0xF, 0x8, 0x8)
  val Green = new ColorRGB4Bit(0x3, 0xE, 0x6)
  val BrightGreen = new ColorRGB4Bit(0x7, 0xF, 0xA)
  val Yellow = new ColorRGB4Bit(0xE, 0xE, 0x6)
  val BrightYellow = new ColorRGB4Bit(0xF, 0xF, 0xB)
  val Blue = new ColorRGB4Bit(0xB, 0x9, 0xF)
  val BrightBlue = new ColorRGB4Bit(0xD, 0xA, 0xF)
  val Magenta = new ColorRGB4Bit(0xE, 0x6, 0xB)
  val BrightMagenta = new ColorRGB4Bit(0xF, 0x9, 0xD)
  val Cyan = new ColorRGB4Bit(0x7, 0xC, 0xD)
  val BrightCyan = new ColorRGB4Bit(0xA, 0xE, 0xE)
  val White = new ColorRGB4Bit(0xE, 0xE, 0xE)
  val BrightWhite = new ColorRGB4Bit(0xF, 0xF, 0xF)
}

package board.display

import chisel3._
import chisel3.util._
import chisel3.experimental.requireIsChiselType

private object Top {
  val ColorPaletteScheme = DraculaPaletteModified
}

class ConsoleCharBundle extends Bundle {
  val color = UInt(4.W)
  val colorBg = UInt(4.W)
  val char = UInt(8.W)

  def pack: UInt = asUInt
}

object ConsoleCharBundle {
  def packedWidth: Int = 4 + 4 + 8
//  def asBundle(color: UInt, char: UInt): ConsoleCharBundle = {
//    val bundle = Wire(new ConsoleCharBundle)
//    bundle.color := color
//    bundle.char := char
//    bundle
//  }
  def unpack(data: UInt): ConsoleCharBundle = data.asTypeOf(new ConsoleCharBundle())
  def pack(bundle: ConsoleCharBundle): UInt = bundle.pack
}

class Console(params: VGAParams) extends Module {
  private def wireOf[T <: Data](gen: T, value: T): T = {
    requireIsChiselType(gen, "wireof")
    val wire = Wire(gen)
    wire := value
    wire
  }

  val CoordinateType = SInt(params.coordinateWidth.W)
  val DELAY = 2
  val CONSOLE_CHAR_WIDTH = 8
  val CONSOLE_CHAR_HEIGHT = 16
  val CHAR_COLS = params.horizontal.visibleArea / CONSOLE_CHAR_WIDTH
  val CHAR_ROWS = params.vertical.visibleArea / CONSOLE_CHAR_HEIGHT

  require(CHAR_COLS < 256)  // 8 bits
  require(CHAR_ROWS < 64)   // 6 bits

  val io = IO(new Bundle {
    val info = Flipped(new VGAInfoBundle(params.coordinateWidth))

    val charRamAddr = Output(UInt(14.W))
    val charRamData = Input(UInt(ConsoleCharBundle.packedWidth.W))

    val fontRomAddr = Output(UInt(12.W))
    val fontRomData = Input(UInt(8.W))

    val out = Output(new RGB4BitBundle())
  })

  val paletteFg = Module(new ColorPalette())
  val paletteBg = Module(new ColorPalette())

  val x = io.info.x + DELAY.S

  // 1 cycle delay: read character from buffer (RAM)
  val charX = wireOf(UInt(8.W), (x >> 3).asUInt)
  val charY = wireOf(UInt(6.W), (io.info.y >> 4).asUInt)
  io.charRamAddr := Cat(charY, charX)  // get the char should display in (x, y)
  val charData = ConsoleCharBundle.unpack(io.charRamData)
  paletteFg.io.idx := charData.color
  paletteBg.io.idx := charData.colorBg

  // 1 cycle delay: read font data from ROM
  val charLine = io.info.y(3, 0)
  io.fontRomAddr := Cat(charData.char, charLine)

  val pixelEnable = io.fontRomData(io.info.x(2, 0))
  io.out :<= Mux(pixelEnable, paletteFg.io.out, paletteBg.io.out)
}

object ConsoleCharState extends ChiselEnum {
  val Idle = Value
}

class ColorPalette extends Module {
  val io = IO(new Bundle {
    val idx = Input(UInt(4.W))
    val out = Output(new RGB4BitBundle())
  })

  val table = Seq(
    0x0 -> Top.ColorPaletteScheme.Black,
    0x1 -> Top.ColorPaletteScheme.Red,
    0x2 -> Top.ColorPaletteScheme.Green,
    0x3 -> Top.ColorPaletteScheme.Yellow,
    0x4 -> Top.ColorPaletteScheme.Blue,
    0x5 -> Top.ColorPaletteScheme.Magenta,
    0x6 -> Top.ColorPaletteScheme.Cyan,
    0x7 -> Top.ColorPaletteScheme.White,
    0x8 -> Top.ColorPaletteScheme.BrightBlack,
    0x9 -> Top.ColorPaletteScheme.BrightRed,
    0xA -> Top.ColorPaletteScheme.BrightGreen,
    0xB -> Top.ColorPaletteScheme.BrightYellow,
    0xC -> Top.ColorPaletteScheme.BrightBlue,
    0xD -> Top.ColorPaletteScheme.BrightMagenta,
    0xE -> Top.ColorPaletteScheme.BrightCyan,
    0xF -> Top.ColorPaletteScheme.BrightWhite,
  ).map(o => o._1.U(io.idx.getWidth.W) -> o._2.asChiselBundle)

  io.out := MuxLookup(io.idx, table.head._2)(table)
}

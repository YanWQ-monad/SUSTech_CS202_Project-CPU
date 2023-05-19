package board.display

import chisel3._
import chisel3.util._
import chisel3.experimental._

private object Top {
  val ColorPaletteScheme = DraculaPalette
}

class ConsoleCharBundle extends Bundle {
  val color = UInt(3.W)
  val char = UInt(8.W)

  def pack: UInt = asUInt
}

object ConsoleCharBundle {
  def packedWidth: Int = 3 + 8
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

  val palette = Module(new ColorPalette())
  val background = Top.ColorPaletteScheme.Background.asChiselBundle

  val x = io.info.x + DELAY.S

  // 1 cycle delay
  val charX = wireOf(UInt(8.W), (x >> 3).asUInt)
  val charY = wireOf(UInt(6.W), (io.info.y >> 4).asUInt)
  io.charRamAddr := Cat(charY, charX)
  val charData = ConsoleCharBundle.unpack(io.charRamData)
  palette.io.idx := charData.color

  // 1 cycle delay
  val charLine = io.info.y(3, 0)
  io.fontRomAddr := Cat(charData.char, charLine)

  val pixelEnable = io.fontRomData(io.info.x(2, 0))
  io.out :<= Mux(pixelEnable, palette.io.out, background)
}

object ConsoleCharState extends ChiselEnum {
  val Idle = Value
}

class ColorPalette extends Module {
  val io = IO(new Bundle {
    val idx = Input(UInt(3.W))
    val out = Output(new RGB4BitBundle())
  })

  val table = Seq(
    0 -> Top.ColorPaletteScheme.Foreground,
    1 -> Top.ColorPaletteScheme.Cyan,
    2 -> Top.ColorPaletteScheme.Green,
    3 -> Top.ColorPaletteScheme.Orange,
    4 -> Top.ColorPaletteScheme.Pink,
    5 -> Top.ColorPaletteScheme.Purple,
    6 -> Top.ColorPaletteScheme.Red,
    7 -> Top.ColorPaletteScheme.Yellow,
  ).map(o => o._1.U(io.idx.getWidth.W) -> o._2.asChiselBundle)

  io.out := MuxLookup(io.idx, table.head._2)(table)
}

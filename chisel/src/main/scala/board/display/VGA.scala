package board.display

import chisel3._

case class OneTiming(
                      visibleArea: Int,
                      frontPorch: Int,
                      syncPulse: Int,
                      backPorch: Int,
                    ) {
  def wholeLine: Int = visibleArea + frontPorch + syncPulse + backPorch

  def lineStart: Int = 0 - frontPorch - syncPulse - backPorch
  def syncStart: Int = lineStart + frontPorch
  def syncEnd: Int = syncStart + syncPulse
  def activeStart: Int = 0
  def lineEnd: Int = visibleArea
}

case class VGAParams(
                      horizontal: OneTiming,
                      vertical: OneTiming,
                      coordinateWidth: Int,
                    )

class VGAOutBundle extends Bundle {
  val hsync = Output(Bool())
  val vsync = Output(Bool())
  val data = Output(new RGB4BitBundle())
}

class VGAInfoBundle(coordinateWidth: Int) extends Bundle {
  val frameSignal = Output(Bool())
  val lineSignal = Output(Bool())
  val x = Output(SInt(coordinateWidth.W))
  val y = Output(SInt(coordinateWidth.W))
}

class VGATop(params: VGAParams) extends Module {
  val io = IO(new Bundle {
    val out = new VGAOutBundle()
    val info = new VGAInfoBundle(params.coordinateWidth)
    val in = Input(new RGB4BitBundle())
  })

  val timing = Module(new VGATiming(params))
  io.out.hsync := timing.io.hsync
  io.out.vsync := timing.io.vsync
  io.out.data := Mux(timing.io.dataEnable, io.in, RGB4BitBundle.Black)

  io.info <> timing.io.info
}

class VGATiming(params: VGAParams) extends Module {
  val CoordinateType = SInt(params.coordinateWidth.W)

  val io = IO(new Bundle {
    val hsync = Output(Bool())
    val vsync = Output(Bool())
    val dataEnable = Output(Bool())
    val info = new VGAInfoBundle(params.coordinateWidth)
  })

  val x = RegInit(CoordinateType, params.horizontal.lineStart.S)
  val y = RegInit(CoordinateType, params.vertical.lineStart.S)
  val trueY = RegInit(CoordinateType, params.vertical.lineStart.S)  // use trueY, so vsync will appear correctly

  when(x === -1.S) {  // only when x is going to be 0, update trueY
    trueY := y
  }

  when(x === (params.horizontal.lineEnd - 1).S) {
    val frameEnd = y === (params.vertical.lineEnd - 1).S
    y := Mux(frameEnd, params.vertical.lineStart.S, y + 1.S)
    x := params.horizontal.lineStart.S
  }.otherwise {
    x := x + 1.S
  }

  io.hsync := RegNext(!(x >= params.horizontal.syncStart.S && x < params.horizontal.syncEnd.S))
  io.vsync := RegNext(!(trueY >= params.vertical.syncStart.S && trueY < params.vertical.syncEnd.S))
  io.dataEnable := RegNext(x >= params.horizontal.activeStart.S && y >= params.vertical.activeStart.S)
  io.info.frameSignal := RegNext(x === params.horizontal.lineStart.S && y === params.vertical.lineStart.S)
  io.info.lineSignal := RegNext(x === params.horizontal.lineStart.S)

  io.info.x := RegNext(x)
  io.info.y := RegNext(y)
}

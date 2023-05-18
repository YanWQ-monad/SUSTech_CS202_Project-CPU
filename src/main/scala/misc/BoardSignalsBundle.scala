package misc

import chisel3._

class BoardDataBundle extends Bundle {
  val uartTx = Output(Bool())
  val uartRx = Input(Bool())
}

class RealBoardDataBundle extends Bundle {
  val uartTx = Output(Bool())
  val uartRx = Input(Bool())
}

class BoardDataController extends Module {
  val outer = IO(new RealBoardDataBundle())
  val inside = IO(new BoardDataBundle())
}

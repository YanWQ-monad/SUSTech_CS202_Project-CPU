源码在 [src/main/scala](src/main/scala) 文件夹下。

## 目录结构

|                                   文件(夹)                                 |   备注   |
|---------------------------------------------------------------------------|----------|
| [Top.scala](src/main/scala/Top.scala)                                     | 顶层模块 |
| [core/](src/main/scala/core)                                              | CPU 核心部分 |
| &emsp; [Core.scala](src/main/scala/core/Core.scala)                       | CPU 核心部分的顶层模块（主要是连线） |
| &emsp; [Decoder.scala](src/main/scala/core/Decoder.scala)                 | ID 模块，从指令解码出各种控制线的值（有点像 control） |
| &emsp; [Memory.scala](src/main/scala/core/Memory.scala)                   | 内存部分，包括翻译 MMIO 地址 |
| &emsp; [ALU.scala](src/main/scala/core/ALU.scala)                         | ALU 模块 |
| &emsp; [Comparator.scala](src/main/scala/core/Comparator.scala)           | 比较（大于小于等）模块，RISC-V 需要 |
| &emsp; [RegisterFile.scala](src/main/scala/core/RegisterFile.scala)       | 寄存器表 |
| [board/](src/main/scala/board)                                            | 硬件 IO 部分（把 CPU 的内存操作翻译到板子需要的 IO 信号，反之亦然） |
| &emsp; [Board.scala](src/main/scala/board/Board.scala)                    | IO 部分的顶层模块（主要也是连线） |
| &emsp; [display/](src/main/scala/board/display)                           | VGA 显示 |
| &emsp;&emsp; [Console.scala](src/main/scala/board/display/Console.scala)  | VGA 核心部分，从缓冲区读取字符并渲染出来 |
| &emsp;&emsp; [VGA.scala](src/main/scala/board/display/VGA.scala)          | VGA 屏幕参数和 Timing 部分 |
| &emsp;&emsp; [Color.scala](src/main/scala/board/display/Color.scala)      | 16 色调色板 |
| &emsp; [UART.scala](src/main/scala/board/UART.scala)                      | UART |
| &emsp; [Keyboard.scala](src/main/scala/board/Keyboard.scala)              | 小键盘 |
| &emsp; [Tubes.scala](src/main/scala/board/Tubes.scala)                    | 7 段数码管 |
| [ip/](src/main/scala/ip)                                                  | 用 BlackBox 来对接 Vivado 的 IP 核 |
| &emsp; [BlockMemory.scala](src/main/scala/ip/BlockMemory.scala)           | Block Memory（双端口读写） |
| &emsp; [BlockMemoryRom.scala](src/main/scala/ip/BlockMemoryRom.scala)     | Block Memory (ROM) |
| &emsp; [ClockWizard.scala](src/main/scala/ip/ClockWizard.scala)           | Clock Wizard |
| [misc/](src/main/scala/misc)                                              | 一些杂七杂八的模块 |
| &emsp; [CrossClockQueue.scala](src/main/scala/misc/CrossClockQueue.scala) | 用于跨时钟传输数据的循环队列 |
| &emsp; [Debounce.scala](src/main/scala/misc/Debounce.scala)               | 消抖模块 |
| &emsp; [RNG.scala](src/main/scala/misc/RNG.scala)                         | 基于 LFSR 的随机数生成器 |
| [util/](src/main/scala/util)                                              | 一些杂七杂八的 util 函数（一般与模块无关） |

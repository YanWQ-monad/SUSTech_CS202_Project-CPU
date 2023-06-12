<div align="center">

# SUSTech CS202 Project (Computer Organization)<br>Naïve RISC-V CPU
  
</div>

![](https://github.com/YanWQ-monad/SUSTech_CS202_Project-CPU/assets/20324409/d366a6d6-91e1-4d1f-aff7-26cf9c334b64)

## 功能

### CPU 核心

- 指令集：RISC-V RV32I 子集（`FENCE`, `ECALL`, `EBREAK` 除外）
- 准单周期（等效运行频率 20MHz）
- 内存：128 KiB（从 0x00000000 到 0x00020000）
- 通过 MMIO 进行 I/O 操作


### I/O

- 手写 UART，且支持程序通过 UART 进行输入输出
  - UART 参数：比特率 115200，8 位 Data Bits，2 位 Stop Bits。128 字节缓冲区
  - 支持在不重新烧写 FPGA 的情况下加载不同程序（通过软件加载软件）
- 支持 24 个开关、LED，5 个按钮
- 支持 4×4 矩阵小键盘，以及 7 段数码管
- 支持 VGA 输出，800×600 60Hz
  - 单个字符大小为 8×16 像素，一个屏幕总共能容纳 100×37 个字符
  - 字符的前景背景各支持 16 种颜色（ANSI Color）

<details>
  <summary> Memory-mapped I/O 地址（点击展开） </summary>

  - `0xFFFFF000`(`r`)：从 UART 读取 1 个字符
  - `0xFFFFF004`(`w`)：向 UART 写入 1 个字符
  - `0xFFFFF008`(`r`)：UART 读 ready（缓冲区是否不为空）
  - `0xFFFFF00C`(`r`)：UART 写 ready（缓冲区是否不满）
  - `0xFFFFF010`(`r`)：当前的 cycles（32 位）
  - `0xFFFFF014`(`r`)：读取小键盘的 1 次输入
  - `0xFFFFF018`(`r`)：小键盘是否有输入（缓冲区是否不为空）
  - `0xFFFFF01C`(`r`)：按键 center 是否按下
  - `0xFFFFF020`(`r`)：按键 up 是否按下
  - `0xFFFFF024`(`r`)：按键 down 是否按下
  - `0xFFFFF028`(`r`)：按键 left 是否按下
  - `0xFFFFF02C`(`r`)：按键 right 是否按下
  - `0xFFFFF030`(`w`)：数码管模式（0: 10 进制，1: 16 进制）
  - `0xFFFFF038`(`w`)：数码管数值
  - `0xFFFFF03C`(`w`)：数码管每个数位的 enable（用 0-7 共 8 个二进制位表示某个数码管是否显示）
  - `0xFFFFF040`(`r`)：中断前的 PC ~~（水分用的）~~
  - `0xFFFFF044`(`r`)：随机数发生器
  - `0xFFFFF1__`(`r`)：24 个 switches（每个 switch 占 4 byte）
  - `0xFFFFF2__`(`w`)：24 个 LED（每个 LED 占 4 byte）
  - `0xFFFD____`(`rw`)：VGA 的字符缓冲区
  - `0xFFFE____`(`rw`)：VGA 的颜色缓冲区  
    注：VGA 缓冲区大小为 256×64 个字符，只有左上角符合屏幕的部分会显示。每个字符的地址均为 4 字节对齐。  
    例：字符 `(x,y) = (5,3)`（从 0 开始）的地址为 `0xFFFD0C14`。
</details>


### 其它
- 软件部分使用 Rust 进行编写，通过交叉编译编译到二进制后可加载到开发板上
  - 移植了 [tetrs](https://github.com/freymo/tetrs) 俄罗斯方块，能成功运行
- 使用 Verilator 进行仿真，在此基础上：
  - 编写差分测试（支持模拟 UART I/O），通过差分测试验证正确性（此时仿真速度约为每秒 20 万周期）
  - 调用 SDL 库绘制 VGA 输出，可以在不上板的情况下粗略测试 VGA 输出是否正确

## 目录结构

|                        文件(夹)                   |   备注   |
|--------------------------------------------------|---------|
| [chisel/](chisel)                                | Chisel 源代码 |
| [font/](font)                                    | VGA 字符字体（基于 Ark Pixel） |
| [program/](program)                              | 软件部分（使用 Rust 编写） |
| [simulation/](simulation)                        | 仿真必要文件（含差分测试） |
| [vivado/](vivado)                                | Vivado 必要文件 |
| &emsp; [constraints.xdc](vivado/constraints.xdc) | Minisys 约束文件 |
| &emsp; [ip/](vivado/ip)                          | Vivado IP 核 |

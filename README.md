<div align="center">

# SUSTech CS202 Project (Computer Organization)<br>Naïve RISC-V CPU
  
</div>

![](https://github.com/YanWQ-monad/SUSTech_CS202_Project-CPU/assets/20324409/a0864ac3-15db-4f80-967f-c922ea654c0d)


## 功能

### CPU 核心

- 指令集：RISC-V RV32I 子集（`FENCE`, `ECALL`, `EBREAK` 除外）
- 准单周期（等效运行频率 20MHz）
- 内存：128 KiB（从 `0x00000000` 到 `0x00020000`）
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
  - `0xFFFFF040`(`r`)：中断前的 PC ~~（骗分用的）~~
  - `0xFFFFF044`(`r`)：随机数发生器
  - `0xFFFFF1__`(`r`)：24 个 switches（每个 switch 占 4 byte）
  - `0xFFFFF2__`(`w`)：24 个 LED（每个 LED 占 4 byte）
  - `0xFFFD____`(`rw`)：VGA 的字符缓冲区
  - `0xFFFE____`(`rw`)：VGA 的颜色缓冲区  
    注：VGA 缓冲区大小为 256×64 个字符，只有左上角符合屏幕的部分会显示。每个字符的地址均为 4 字节对齐。  
    例：字符 `(x,y) = (5,3)`（从 0 开始）的地址为 `0xFFFD0C14`。
</details>


### 其它

- 可以通过板子上 P20 的 reset 按钮进行 reset
  - 此操作会清空电路中大部分 `reg` 的值（内存、寄存器不会清空），然后跳转到 `0x00000000` 的 bootloader 处重新开始执行
- 软件部分使用 Rust 进行编写，通过交叉编译编译到二进制后可加载到开发板上
  - 移植了 [tetrs](https://github.com/freymo/tetrs) 俄罗斯方块，能成功运行
- 使用 Verilator 进行仿真，在此基础上：
  - 编写差分测试（支持模拟 UART I/O），通过差分测试验证正确性（此时仿真速度约为每秒 20 万周期）
  - 调用 SDL 库绘制 VGA 输出，可以在不上板的情况下粗略测试 VGA 输出是否正确
- 时钟频率：
  - CPU 核心：40 MHz
  - UART：12.5 MHz（再手动用 151/1024 分频到 1.84 MHz，此频率约为 115200 Hz 的 16 倍）
  - VGA：40 MHz


## 目录结构

|                        文件(夹)                   |   备注   |
|--------------------------------------------------|----------|
| [chisel/](chisel)                                | **硬件部分**（Chisel 代码） |
| [program/](program)                              | **软件部分**（使用 Rust 编写） |
| [font/](font)                                    | VGA 字符字体（基于 Ark Pixel） |
| [simulation/](simulation)                        | 仿真相关文件（含差分测试） |
| [vivado/](vivado)                                | Vivado 必要文件 |
| &emsp; [constraints.xdc](vivado/constraints.xdc) | Minisys 约束文件 |
| &emsp; [ip/](vivado/ip)                          | Vivado IP 核 |
| [generated/](generated)                          | 一些编译好的东西，应该可以直接用 |


## 使用

~~虽然可能有亿点点麻烦~~

1. 编译 program 目录下的软件，得到 `loader.coe`，它是主内存的初始化文件。
2. 运行 chisel 目录下的 Chisel 项目，得到 `Top.sv`，它就是接下来喂给 Vivado 的 Verilog 文件。
3. 从 font 目录下的 `font.txt` 生成一个 coe 文件（加一个文件头和一堆逗号就行），用作初始化 Font ROM。
4. 新建 Vivado 项目，导入 vivado 目录下的 IP 核心以及约束文件，并且指定好上述两个 coe 文件。
5. 导入第 2 步生成的 `Top.sv`，综合、实现、生成比特流，就可以烧板了。
6. 连接 UART，就可以加载程序：首先发送一个整数表示程序长度，然后再发送相应长度的二进制内容，然后 bootloader 就会运行它。

或者可以直接用编译好的，见 [generated](generated)。

## 碎碎念

虽然 CPU 本身并没有写得有多好，像 pipeline、cache 那些什么的，都没啥时间写（整个项目基本上是最后一周才动工的）。
不过至少个人感觉在开发舒适度方面（指开发、调试环境方面），感觉还是有些东西能分享一下的。

---

~~“如果觉得 Verilog 写得很痛苦，那么一定是方法没用对” ——我~~

首先写 Verilog 确实痛苦，但是我们有更好的选择：  
可以用 Scala 的 Chisel 框架写代码，然后再调用框架将 Scala 编译运行然后生成 Verilog 代码，这个 Verilog 代码是可以直接扔到 Vivado 编译的。
这样有什么好处呢，因为 Scala 是一门类似于 Java 的高级语言，并且 Chisel 也提供了很多好用的“语法糖”，可以以比较舒服的方式去描述硬件电路。  
**例如**在 Verilog 中，为了将两个模块的端口连接起来，要先声明一个 `wire`，再分别绑定到两个模块，这样在实际写程序的时候就十分麻烦，而且很容易打断思路、写出 bug。
而在 Chisel 中，一个 `dut1.x := dut2.x` 赋值就完成了，没有其它乱七八糟的东西，写起来就十分流畅。  
而且 Chisel 在编译的时候，还可以提前帮忙检查代码的问题（例如未绑定端口），这样生成的 Verilog 代码基本上是可以一次过 synthesis 和 implementation，就不用和 Vivado 打很多交道。

---

再者就是软件的编写。众所周知，写汇编语言十分痛苦 ~~（应该没有人享受写汇编语言吧）~~。
并且解决问题的最好方法就是解决问题本身——不写汇编不就行了？  
现在有很多编程语言（例如 C++、Rust）都提供交叉编译功能，即可以在自己的电脑上写 C++ 或者 Rust 代码，
然后编译器可以将代码编译到另一个架构（比如说，Mips）的二进制文件，就能直接放在 CPU 运行了。
~~（其实计组 OJ 也可以如法[炮制](https://lab.victorica.dev/ptilopsis)）~~
毕竟写高级语言非常舒服，而且编译器的编译肯定是不会出错的（退一万步来说，至少比我强），所以写起来就很舒服，不用担心汇编那一堆乱七八糟的东西。  
当然，过程中可能还要学会使用一些别的东西，比如说链接脚本。~~如果有时间的话我看看能不能写一个 tutorial。~~

~~然后在上面两者的加持下，整个项目我应该写了不到 50 行的 Verilog 和不到 50 行的汇编。
（看右边 GitHub 的统计信息，也可以发现 Verilog 和汇编的占比确实不多。）~~

当然也是多亏了交叉编译，我才能把俄罗斯方块移植到这个 CPU 上，不然手写汇编肯定是会疯掉的。

---

然后到了测试部分，众所周知，测试 CPU 写得对不对也是一个十分痛苦的过程。

> 虚假的测试：  
> 假设有位幸运儿，他选择上计组 OJ 提交代码评测，他首先需要学会那一堆 IP 怎么提交，然后千辛万苦交上去了，发现 WA 一个点，他还不知道它是怎么错的，只能靠猜，或者在群里大海捞针；
> 接下来又过了若干个小时，终于把 OJ 的测试点过了，然后他非常兴奋地复制粘贴到 proj 上面去，
> 但是很不幸，在连线的时候，不小心连错了一个地方，Vivado 没有报错，生成比特流然后上板，它炸了，他不知道怎么入手调试，只能盲人摸象……

所以我们需要一个更加优秀方便舒服的手段来调试。因为众所周知，上板调试十分痛苦，因为“最好方法就是解决问题本身”，所以就是不要上板，靠仿真！
仿真可以通过一些手段加载程序执行，然后可以通过看波形图，去分析每条指令执行得对不对。
诶等等，不对不对，手动看几百条指令，还有那么多条波形，这也没方便到哪里去吧，还很容易看漏。  
所以我们就要把这个检查的过程交给程序来做。这里推荐“差分测试”。
首先在 Verilator 仿真器下 ~~（Vivado 你\*\*谁）~~，可以通过自己编写 C++ 代码和仿真器进行交互，当然也可以获取到每条信号的值是多少。
然后只要把这些信号的值和标准的值对比一下，就知道对不对了。
那…这个“标准的值”哪来呢？这里可以用 unicorn 仿真器（这两个“仿真器”的意思是有点区别的），它是一个开源的轻量级的支持 RISC-V 32 位的仿真器，~~几千 star 的开源项目肯定靠谱（至少比自己写的靠谱）~~。
于是我们就可以在每一轮，首先让自己的 CPU 跑一个周期，再让仿真器跑一个周期，然后对比一下两者的寄存器的值是否一致，如果不一致就打印出当前指令的细节，然后停止。
这样就可以非常方便地定位到代码错在哪个地方，而且还可以生成波形图，通过比较错误位置的一些信号的值，就可以很容易知道是哪里错了。
这…不比上板调试来得舒服多了？

---

~~唔，虽然写了很多，但是感觉都是很笼统的东西，放假有时间了希望能把这些东西写几篇仔细的 tutorial 出来。~~


## 致谢

- [tetrs](https://github.com/freymo/tetrs)，一个用 Rust 编写的命令行俄罗斯方块。
  本项目把图形输出部分适配到本 CPU 上，并且只保留了其核心游戏部分。
- [Project F](https://projectf.io/)，参考了 Display Signals 中部分信号的设计，以及参考了 VGA 仿真部分。
- [fpga4fun - Serial interface](https://www.fpga4fun.com/SerialInterface.html)，UART 模块原型。
- [方舟像素字体](https://github.com/TakWolf/ark-pixel-font)，其 16px 等宽的 "Basic Latin" 部分用作 VGA 显示的字体。
- [rCore Tutorial Book](https://rcore-os.cn/rCore-Tutorial-Book-v3/index.html)，参考借鉴了它的一些代码来搭建 Rust 交叉编译环境。
- [DiffTest](https://oscpu.github.io/ysyx/events/2021-07-17_Difftest/DiffTest-一种高效的处理器验证方法.pdf)，一份关于差分测试的介绍。
- ~~[VGA Tangram](https://github.com/YanWQ-monad/SUSTech_CS207_Project-Tangram)，我自己的数字逻辑的项目，有些 VGA 和数码管的代码就是参考自这里。~~

框架部分：

- [Unicorn Engine](https://www.unicorn-engine.org/)，是本项目中差分测试的标准参考。
- [Verilator](https://www.veripool.org/verilator/)，高效 Verilog 仿真器。
- [Chisel](https://www.chisel-lang.org/)，“Chisel/FIRRTL Hardware Compiler Framework”。

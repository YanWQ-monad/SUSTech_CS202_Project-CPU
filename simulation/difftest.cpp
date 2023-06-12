#include <unicorn/unicorn.h>
#include <vector>
#include <fstream>
#include <queue>
#include <cstdlib>
#include <ctime>
using std::vector;
using std::deque;

#include "verilated_vcd_c.h"
#include "verilated.h"
#include "verilated_vpi.h"

#include "VTop.h"

const char *REG_NAMES[] = {"x0", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"};
constexpr int BASE_ADDR = 0x00000000;
constexpr int H_RES = 800;
constexpr int V_RES = 600;

uc_engine *uc;
deque<int> q1;
deque<int> q2;
deque<int> qc;

s_vpi_value vpi_v;
vpiHandle vpi_mem_deque;
// vpiHandle vpi_uartIn_bits;
vpiHandle vpi_uartIn_valid;
vpiHandle vpi_uartOut_ready;
vpiHandle vpi_pc_old;
vpiHandle vpi_cycles;
vpiHandle vpi_registers[32];

// 获取 handle 的值
int get_value_of(vpiHandle vh) {
    vpi_get_value(vh, &vpi_v);
    return vpi_v.value.integer;
}

// 获取 handle
vpiHandle get_handle(const char *name) {
    vpiHandle vh = vpi_handle_by_name((PLI_BYTE8*)name, NULL);
    if (!vh) {
        printf("name: %s\n", name);
        vl_fatal(__FILE__, __LINE__, "get_handle", "No handle found");
    }
    return vh;
}

int get_value_of(const char *name) {
    return get_value_of(get_handle(name));
}

void update_emu() {
    int v;
    v = get_value_of(vpi_mem_deque);
    uc_mem_write(uc, 0xFFFFF000, (char*)&v, 4);
    v = get_value_of(vpi_uartIn_valid);
    uc_mem_write(uc, 0xFFFFF008, (char*)&v, 4);
    v = get_value_of(vpi_uartOut_ready);
    uc_mem_write(uc, 0xFFFFF00C, (char*)&v, 4);
}

bool read_and_check(int pc) {
    bool same = true;

    same = (get_value_of(vpi_pc_old) == pc);
    for (int i = 0, v2; i < 32; i++) {
        int v1 = get_value_of(vpi_registers[i]);  // 获取 FPGA 第 i 个寄存器的值
        uc_reg_read(uc, i + 1, &v2);              // 获取 unicorn 第 i 个寄存器的值
        if (v1 != v2)
            same = false;
    }

    if ((!same)) {  // 如果有不相同的，就打印信息
        printf("FPGA PC = 0x%08x, cycles = 0x%08x\n", get_value_of(vpi_pc_old), get_value_of(vpi_cycles));
        for (int i = 0; i < 32; i++) {
            printf("  %3s: %08x", REG_NAMES[i], get_value_of(vpi_registers[i]));
            if ((i + 1) % 8 == 0)
                puts("");
        }

        printf("unicorn PC = 0x%08x\n", pc);
        for (int i = 0, j; i < 32; i++) {
            uc_reg_read(uc, i + 1, &j);
            printf("  %3s: %08x", REG_NAMES[i], j);
            if ((i + 1) % 8 == 0)
                puts("");
        }

        scanf("%*c");

        return true;
    }
    return true;
}

vector<char> read_binary(const char *name) {
    std::ifstream f;
    f.open(name, std::ios::binary);
    f.seekg(0, std::ios::end);
    size_t size = f.tellg();

    std::vector<char> data;
    data.resize(size);
    f.seekg(0, std::ios::beg);
    f.read(&data[0], size);
    return data;
}

void send_uart_char(const char ch) {
    qc.push_back((int) (unsigned char) ch);

    q1.push_back(0);
    int c = (int) (unsigned char) ch;
    for (int i = 0; i < 8; i++) {
        q1.push_back(c & 1);
        c >>= 1;
    }
    q1.push_back(1);
    q1.push_back(1);
}

void send_uart_string(const char *s) {
    while (*s)
        send_uart_char(*s++);
}

int pop_uart_bit() {
    static int current = 1;
    if (q2.empty()) {
        if (q1.empty())
            return current;

        int b = q1.front();
        // 0 ~ 20  ->  -10 ~ 10
        int delta =  + ((rand() % 41) / 10 - 2);
        for (int i = 0; i < 4 * 16 + delta; i++)
            q2.push_back(b);
        q1.pop_front();
    }

    int t = q2.front();
    q2.pop_front();
    return current = t;
}

int main(int argc, char *argv[]) {
    Verilated::commandArgs(argc, argv);
    srand((unsigned)time(nullptr));


    // 1. 初始化 unicorn
    uc_err err;   //     RISC-V          32-bit
    if ((err = uc_open(UC_ARCH_RISCV, UC_MODE_32, &uc)) != UC_ERR_OK) {
        printf("Failed on uc_open() with error returned: %u\n", err);
        return -1;
    }
    uc_mem_map(uc, BASE_ADDR, 2 * 1024 * 1024, UC_PROT_ALL);   // allocate main memory
    uc_mem_map(uc, 0xFFFC0000, 4096 * 64, UC_PROT_ALL);        // allocate MMIO memory
    vector<char> data = read_binary("loader.bin");
    if (uc_mem_write(uc, BASE_ADDR, data.data(), data.size())) {   // 加载 loader
        printf("Failed to write emulation code to memory, quit!\n");
        return -1;
    }
    vector<char> data2 = read_binary("main.bin");
    if (uc_mem_write(uc, 0x00008000, data2.data(), data2.size())) {   // 加载 main
        printf("Failed to write emulation code to memory, quit!\n");
        return -1;
    }


    int tracing = 0;
    int uart_bits = 0;

    // 仿真：实例化 Top 模块，并开启波形图
    // 可以参考 verilator 文档
    const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
    Verilated::traceEverOn(true);
    VerilatedVcdC* tfp = new VerilatedVcdC;
    VTop *top = new VTop(contextp.get(), "TOP");
    top->trace(tfp, 1);
    tfp->open("simx4.vcd");

    // 为了方便，这里先获取 FPGA 内部的线的实例
    char buffer[64];
    vpi_v.format = vpiIntVal;
    vpi_pc_old = get_handle("TOP.Top.core.pcOld");
    vpi_cycles = get_handle("TOP.Top.core.cycles");
    vpi_mem_deque = get_handle("TOP.Top.core.mem.inner.dequeueData");
    vpi_uartIn_valid = get_handle("TOP.Top.core.mem.io_external_uartIn_valid");
    vpi_uartOut_ready = get_handle("TOP.Top.core.mem.io_external_uartOut_ready");
    // vpiHandle vh_uart_tick = get_handle("TOP.Top.board.uart.uart.tx.tick");
    // vpi_uartIn_bits = get_handle("TOP.Core.mem.io_external_uartIn_bits");
    // vpiHandle vh_rx_valid = get_handle("TOP.Top.board.uart.uart.rx.io_valid");
    // vpiHandle vh_rx_bits = get_handle("TOP.Top.board.uart.uart.rx.io_bits");
    for (int i = 0; i < 32; i++) {
        snprintf(buffer, 64, "TOP.Top.core.reg_0.mem_ext.Register%d", i);
        vpi_registers[i] = get_handle(buffer);
    }

    top->out_uart_rx = 1;
    top->reset = 1;  // 首先先 reset 一下

    int pc = BASE_ADDR;

    int time = 0;
    int currentTick = 0;
    int lastTick = 0;
    int uartCnt = 0, uartData;

    // 测试 1000000 个时钟周期
    while (time < 1000000 && !Verilated::gotFinish()) {
        if (time > 997)  // 等待 997 个时钟周期后再把 reset = 0
            top->reset = 0;

        // 1. 让 FPGA 先跑一个时钟周期
        top->clock = 0;
        top->eval();
        contextp->timeInc(1);
        tfp->dump(contextp->time());

        top->clock = 1;
        top->eval();
        contextp->timeInc(1);
        tfp->dump(contextp->time());

        time++;
        if (time <= 1000)  // time <= 1000 的时候，CPU 还没有完成暖机（reset = 1）
            continue;

        VerilatedVpi::callValueCbs();

        if (time % 2 == 1) {  // 因为 CPI = 2
            update_emu();     // 同步 MMIO 的信号到 unicorn 中（这里主要是 UART 信号），一般来说可以略过

            // uc_emu_start(uc, 开始地址, 结束地址, 最大运行条数)
            // 最后一个填 1 让它只运行 1 条指令
            if ((err = uc_emu_start(uc, pc, 0xFFFFFFFF, 0, 1)))
                printf("Failed on uc_emu_start() with error returned %u: %s\n", err, uc_strerror(err));

            // 然后开始对比两者状态
            if (!read_and_check(pc))
                break;

            // 然后更新当前 pc 的值，以便执行下一条指令
            uc_reg_read(uc, UC_RISCV_REG_PC, &pc);
        }

        // 多说无益，举个例子

        // currentTick = get_value_of(vh_uart_tick);
        // // currentValid = get_value_of(vh_rx_valid);

        // if (lastTick && !currentTick) {
        //     int bit = !!(top->out_uart_tx);
        //     uartData = (uartData >> 1) | (bit << 7);
        //     if (!uartCnt && !bit) {  // start bit
        //         uartCnt = 1;
        //         uartData = 0;
        //     }
        //     else if (uartCnt > 0) {
        //         if (uartCnt++ == 8) {
        //             printf("%c", (char)uartData);
        //             // printf("%c (%02x)\n", (char)uartData, uartData);
        //             fflush(stdout);
        //             uartCnt = 0;
        //         }
        //     }
        // }

        if (time == 20000) {
            send_uart_string("0\r");
            // send_uart_string("10232\r");
            // vector<char> dat = read_binary("main.bin");
            // for (auto iter = dat.begin(); iter != dat.end(); ++iter)
            //     send_uart_char(*iter);
            send_uart_string("998\r244\r");
        }

        top->out_uart_rx = pop_uart_bit();

        lastTick = currentTick;
    }

    top->final();
    tfp->close();
    delete top;

    uc_close(uc);
    return 0;
}

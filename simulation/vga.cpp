#include <fstream>
#include <queue>
#include <cstdlib>
#include <ctime>
using std::deque;

#include <SDL.h>
#include "verilated_vcd_c.h"
#include "verilated.h"
#include "verilated_vpi.h"

#include "VTop.h"

constexpr int H_RES = 800;
constexpr int V_RES = 600;

typedef struct Pixel {  // for SDL texture
    uint8_t a;  // transparency
    uint8_t b;  // blue
    uint8_t g;  // green
    uint8_t r;  // red
} Pixel;

Pixel screenbuffer[H_RES*V_RES];

uc_engine *uc;
deque<int> q1;
deque<int> q2;
deque<int> qc;

int get_value_of(vpiHandle vh) {
    s_vpi_value v;
    v.format = vpiIntVal;
    vpi_get_value(vh, &v);
    return v.value.integer;
}

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
    if (SDL_Init(SDL_INIT_VIDEO) < 0) {
        printf("SDL init failed.\n");
        return 1;
    }



    SDL_Window*   sdl_window   = NULL;
    SDL_Renderer* sdl_renderer = NULL;
    SDL_Texture*  sdl_texture  = NULL;

    sdl_window = SDL_CreateWindow("FPGA VGA Simulator", SDL_WINDOWPOS_CENTERED,
        SDL_WINDOWPOS_CENTERED, H_RES, V_RES, SDL_WINDOW_SHOWN);
    if (!sdl_window) {
        printf("Window creation failed: %s\n", SDL_GetError());
        return 1;
    }

    sdl_renderer = SDL_CreateRenderer(sdl_window, -1,
        SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);
    if (!sdl_renderer) {
        printf("Renderer creation failed: %s\n", SDL_GetError());
        return 1;
    }

    sdl_texture = SDL_CreateTexture(sdl_renderer, SDL_PIXELFORMAT_RGBA8888,
        SDL_TEXTUREACCESS_TARGET, H_RES, V_RES);
    if (!sdl_texture) {
        printf("Texture creation failed: %s\n", SDL_GetError());
        return 1;
    }

    // reference SDL keyboard state array: https://wiki.libsdl.org/SDL_GetKeyboardState
    const Uint8 *keyb_state = SDL_GetKeyboardState(NULL);

    uint64_t start_ticks = SDL_GetPerformanceCounter();
    uint64_t frame_count = 0;



    int uart_bits = 0;

    const std::unique_ptr<VerilatedContext> contextp{new VerilatedContext};
    VTop *top = new VTop(contextp.get(), "TOP");

    vpiHandle vh_uart_tick = get_handle("TOP.Top.board.uart.uart.tx.tick");
    vpiHandle vh_enable = get_handle("TOP.Top.board.display.vga._timing_io_dataEnable");
    vpiHandle vh_vga_x = get_handle("TOP.Top.board.display.vga.io_info_x");
    vpiHandle vh_vga_y = get_handle("TOP.Top.board.display.vga.io_info_y");

    top->out_uart_rx = 1;
    top->reset = 1;

    int time = 0;
    int currentTick = 0;
    int lastTick = 0;
    int uartCnt = 0, uartData;
    while (!Verilated::gotFinish()) {
        if (time > 999)
            top->reset = 0;

        top->clock = 0;
        top->eval();

        top->clock = 1;
        top->eval();

        VerilatedVpi::callValueCbs();

        int sx = get_value_of(vh_vga_x);
        int sy = get_value_of(vh_vga_y);
        if (get_value_of(vh_enable)) {
            Pixel* p = &screenbuffer[sy * H_RES + sx];
            p->a = 0xFF;
            p->b = top->out_vga_data_b << 4;
            p->g = top->out_vga_data_g << 4;
            p->r = top->out_vga_data_r << 4;
        }

        if (sx == 0 && sy == 599) {
            // check for quit event
            SDL_Event e;
            while (SDL_PollEvent(&e)) {
                if (e.type == SDL_QUIT) {
                    break;
                }
                if (e.type == SDL_KEYDOWN)
                    send_uart_char((char)e.key.keysym.sym);
            }

            if (keyb_state[SDL_SCANCODE_Q]) break;  // quit if user presses 'Q'

            SDL_UpdateTexture(sdl_texture, NULL, screenbuffer, H_RES*sizeof(Pixel));
            SDL_RenderClear(sdl_renderer);
            SDL_RenderCopy(sdl_renderer, sdl_texture, NULL, NULL);
            SDL_RenderPresent(sdl_renderer);
            frame_count++;
        }

        if (time > 1000) {
            currentTick = get_value_of(vh_uart_tick);

            if (lastTick && !currentTick) {
                int bit = !!(top->out_uart_tx);
                // printf("%d", bit);
                uartData = (uartData >> 1) | (bit << 7);
                if (!uartCnt && !bit) {  // start bit
                    uartCnt = 1;
                    uartData = 0;
                }
                else if (uartCnt > 0) {
                    if (uartCnt++ == 8) {
                        printf("%c", (char)uartData);
                        // printf("%c (%02x)\n", (char)uartData, uartData);
                        fflush(stdout);
                        uartCnt = 0;
                    }
                }
            }

            if (time == 20000) {
                send_uart_string("0\r");
            }

            top->out_uart_rx = pop_uart_bit();
            lastTick = currentTick;
        }

        time++;
    }

    uint64_t end_ticks = SDL_GetPerformanceCounter();
    double duration = ((double)(end_ticks-start_ticks))/SDL_GetPerformanceFrequency();
    double fps = (double)frame_count/duration;
    printf("Frames per second: %.1f\n", fps);

    top->final();
    delete top;

    SDL_DestroyTexture(sdl_texture);
    SDL_DestroyRenderer(sdl_renderer);
    SDL_DestroyWindow(sdl_window);
    SDL_Quit();

    // uc_close(uc);
    return 0;
}

    .section .text.entry
    .globl _start
_start:
    li     t0, 0
    li     t1, 0
    li     t2, 0
    li     a0, 0
    li     a1, 0
    li     a2, 0
    li     a3, 0
    li     a4, 0
    li     a5, 0
    li     a6, 0
    li     a7, 0
    li     t4, 0
    li     t5, 0
    li     t6, 0

    move   t3, sp
    la     sp, boot_stack_top
    addi   sp, sp, -16
    sw     ra, 12(sp)
    sw     t3, 8(sp)
    jal    rust_main
    lw     ra, 12(sp)
    lw     sp, 8(sp)
    ret

    .section .bss.stack
    .globl boot_stack_lower_bound
boot_stack_lower_bound:
    .space 4096 * 4
    .globl boot_stack_top
boot_stack_top:


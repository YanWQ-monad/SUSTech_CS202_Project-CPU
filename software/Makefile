TARGET := riscv32i-unknown-none-elf
MODE := release

PROGRAM_SRC_DIR := src/bin
CARGO_TARGET_DIR := target/$(TARGET)/$(MODE)
TARGET_DIR := bin

PROGRAM_SRC := $(wildcard $(PROGRAM_SRC_DIR)/*.rs) $(PROGRAM_SRC_DIR)/tetris.rs
PROGRAM_NAMES := $(notdir $(PROGRAM_SRC))
PROGRAM_ELF := $(addprefix $(CARGO_TARGET_DIR)/,$(filter-out tetris,$(PROGRAM_NAMES:%.rs=%)))
PROGRAM_BIN := $(addprefix $(TARGET_DIR)/,$(PROGRAM_NAMES:%.rs=%.bin))
PROGRAM_TXT := $(addprefix $(TARGET_DIR)/,$(PROGRAM_NAMES:%.rs=%.txt))
PROGRAM_COE := $(addprefix $(TARGET_DIR)/,$(PROGRAM_NAMES:%.rs=%.coe))
PROGRAM_WITH_LOADER := $(addprefix $(TARGET_DIR)/,$(PROGRAM_NAMES:%.rs=%-loader.txt))
PROGRAM_ASM := $(addprefix $(TARGET_DIR)/,$(PROGRAM_NAMES:%.rs=%.asm))

TETRIS_SRC := $(shell find $(PROGRAM_SRC_DIR)/tetris -type f)

LOADER_LINKER := src/linker-loader.ld
PROGRAM_LINKER := src/linker-program.ld

RUST_DEPS := $(filter-out $(shell find $(PROGRAM_SRC_DIR) -type f) $(LOADER_LINKER) $(PROGRAM_LINKER),$(shell find src -type f)) Cargo.toml

ifeq ($(MODE), release)
	MODE_ARG := --release
endif

OBJDUMP := rust-objdump --arch-name=riscv32
OBJCOPY := rust-objcopy --binary-architecture=riscv32

.PHONY: all
all: $(PROGRAM_BIN) $(PROGRAM_TXT) $(PROGRAM_COE) $(PROGRAM_WITH_LOADER) $(PROGRAM_ASM)

$(TARGET_DIR):
	mkdir -p $(TARGET_DIR)

$(PROGRAM_ELF): $(CARGO_TARGET_DIR)/%: $(PROGRAM_SRC_DIR)/%.rs $(RUST_DEPS) $(PROGRAM_LINKER)
	cp $(PROGRAM_LINKER) src/linker.ld
	cargo build $(MODE_ARG) --bin $*
	rm src/linker.ld

$(CARGO_TARGET_DIR)/tetris: $(TETRIS_SRC) $(RUST_DEPS) $(PROGRAM_LINKER)
	cp $(PROGRAM_LINKER) src/linker.ld
	cargo build $(MODE_ARG) --bin tetris --features="alloc"
	rm src/linker.ld

$(CARGO_TARGET_DIR)/loader: $(PROGRAM_SRC_DIR)/loader.rs $(RUST_DEPS) $(LOADER_LINKER)
	cp $(LOADER_LINKER) src/linker.ld
	cargo build $(MODE_ARG) --bin $*
	rm src/linker.ld

$(PROGRAM_ASM): $(TARGET_DIR)/%.asm: $(CARGO_TARGET_DIR)/% | $(TARGET_DIR)
	$(OBJDUMP) -S $< > $@

$(PROGRAM_BIN): $(TARGET_DIR)/%.bin: $(CARGO_TARGET_DIR)/% | $(TARGET_DIR)
	$(OBJCOPY) $< --strip-all -O binary $@
	head -c $$((4 - $$(stat -f %z $@) % 4)) /dev/zero >> $@

$(PROGRAM_TXT): $(TARGET_DIR)/%.txt: $(TARGET_DIR)/%.bin | $(TARGET_DIR)
	xxd -ps -c 4 -e $< | awk '{ print $$2 }' > $@

$(PROGRAM_COE): $(TARGET_DIR)/%.coe: $(TARGET_DIR)/%.txt | $(TARGET_DIR)
	echo 'memory_initialization_radix = 16;\nmemory_initialization_vector =' > $@
	cat $< | awk '{ print $$0"," }' >> $@
	echo '00000000;' >> $@

$(PROGRAM_WITH_LOADER): $(TARGET_DIR)/%-loader.txt: $(TARGET_DIR)/%.txt $(TARGET_DIR)/loader.txt fill.py | $(TARGET_DIR)
	python3 fill.py $(TARGET_DIR)/loader.txt $@
	cat $< >> $@

.PHONY: send
send: bin/$(PROGRAM).bin
	stat -f %z $< | tr '\n' '\r' > /dev/tty.usbserial-120
	cat $< > /dev/tty.usbserial-120

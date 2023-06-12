LDFLAGS := -lunicorn -lSDL2
export LDFLAGS

TARGET ?= main

.PHONY: all
all: obj_dir_$(TARGET)/VTop

obj_dir_$(TARGET)/VTop: obj_dir_$(TARGET)/VTop.mk $(TARGET).cpp
	make OPT_FAST="-Os" -C obj_dir_$(TARGET) -f VTop.mk -j

obj_dir_$(TARGET)/VTop.mk: ../verilog/Top.sv
	verilator --cc --vpi --trace --Mdir obj_dir_$(TARGET) Top.sv --exe $(TARGET).cpp

.PHONY: run
run: all
	./obj_dir_$(TARGET)/VTop

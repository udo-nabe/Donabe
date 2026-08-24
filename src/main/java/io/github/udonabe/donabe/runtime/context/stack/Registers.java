package io.github.udonabe.donabe.runtime.context.stack;

public class Registers {
    private int pc;

    public Registers() {
        this.pc = 0;
    }

    public void incrementPC() {
        pc++;
    }

    public void setPC(int pc) {
        this.pc = pc;
    }

    public int pc() {
        return pc;
    }
}

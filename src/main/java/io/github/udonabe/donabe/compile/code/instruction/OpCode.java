package io.github.udonabe.donabe.compile.code.instruction;

public enum OpCode {
    PUSH(0x01),
    POP(0x02),
    ADD(0x03),
    SUB(0x04),
    MUL(0x05),
    DIV(0x06),
    EQUAL(0x07),
    GREATER(0x08),
    GREATER_EQUAL(0x09),
    LESS(0x0a),
    LESS_EQUAL(0x0b),
    CALL(0x0c),
    INDEX(0x0d),
    JMP(0x0e),
    JMP_FALSE(0x0f),
    JMP_TRUE(0x10),
    NOP(0x11),
    LOAD_CAPTURED(0x12),
    LOAD_LOCAL(0x13),
    LOAD_MEMBER(0x14),
    STORE_CAPTURED(0x15),
    STORE_LOCAL(0x16),
    MAKE_LIST(0x17),
    NOT(0x18),
    PLUS(0x19),
    MINUS(0x1a),
    RETURN(0x1b),
    VRETURN(0x1c);
    private final byte opcode;

    private OpCode(int opcode) {
        if (opcode < 0 || opcode > 255) {
            throw new IllegalArgumentException("Invalid opecode.");
        }
        
        this.opcode = (byte) opcode;
    }

    public byte opcode() {
        return opcode;
    }
}

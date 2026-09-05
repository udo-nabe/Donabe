package io.github.udonabe.donabe.compile.code;

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
    INSTRUCTION(0x0e),
    JMP(0x0f),
    JMP_FALSE(0x10),
    JMP_TRUE(0x11),
    NOP(0x12),
    LOAD_CAPTURED(0x13),
    LOAD_LOCAL(0x14),
    LOAD_MEMBER(0x15),
    STORE_CAPTURED(0x16),
    STORE_LOCAL(0x17),
    MAKE_LIST(0x18),
    NOT(0x19),
    PLUS(0x1a),
    MINUS(0x1b),
    RETURN(0x1c),
    VRETURN(0x1d);
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

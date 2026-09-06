package io.github.udonabe.donabe.compile.code.section;

public sealed interface Section
        permits CodeSection, ConstantPoolSection, IdentifiersSection {
    
    byte CODE_SECTION_TYPE = 0x01;
    byte CONSTANT_POOL_SECTION_TYPE = 0x02;
    byte IDENTIFIERS_SECTION_TYPE = 0x03;

    byte type();

    byte[] content();
}

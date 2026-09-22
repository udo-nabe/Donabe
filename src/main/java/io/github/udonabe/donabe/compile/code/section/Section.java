package io.github.udonabe.donabe.compile.code.section;

public sealed interface Section
        permits InitializationCodeSection, ConstantPoolSection, GlobalIdentifiersSection {
    
    byte CODE_SECTION_TYPE = 0x01;
    byte CONSTANT_POOL_SECTION_TYPE = 0x02;
    byte GLOBAL_IDENTIFIERS_SECTION_TYPE = 0x03;

    byte type();

    byte[] content();
}

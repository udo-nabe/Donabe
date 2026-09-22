package io.github.udonabe.donabe.compile.code.section;

public sealed interface Section
        permits InitializationCodeSection, ConstantPoolSection, GlobalIdentifiersSection {
    byte[] content();
}

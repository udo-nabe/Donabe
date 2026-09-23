package io.github.udonabe.donabe.compile.code;

import io.github.udonabe.donabe.compile.code.section.ConstantPoolSection;
import io.github.udonabe.donabe.compile.code.section.GlobalIdentifiersSection;
import io.github.udonabe.donabe.compile.code.section.InitializationCodeSection;
import io.github.udonabe.donabe.compile.code.section.Section;
import java.util.HashSet;
import java.util.Set;

public record ByteCode(GlobalIdentifiersSection globalIdentifiersSection,
        ConstantPoolSection constantPoolSection,
        InitializationCodeSection initializationCodeSection) {    
}

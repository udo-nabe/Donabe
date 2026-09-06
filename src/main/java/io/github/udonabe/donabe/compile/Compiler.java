package io.github.udonabe.donabe.compile;

import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.compile.code.ProgramConverter;
import io.github.udonabe.donabe.compile.code.section.IdentifiersSection;
import io.github.udonabe.donabe.ir.IRProgram;
import java.util.Set;

public final class Compiler {

    public ByteCode compile(IRProgram program, Set<Integer> slots) {
        ProgramConverter programConverter = new ProgramConverter();

        ByteCode byteCode = programConverter.generate(program);

        IdentifiersSection identifiers = new IdentifiersSection(slots);
        byteCode = byteCode.addSection(identifiers);
        
        return byteCode;
    }
}

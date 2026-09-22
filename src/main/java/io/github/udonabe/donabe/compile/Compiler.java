package io.github.udonabe.donabe.compile;

import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.compile.code.ProgramConverter;
import io.github.udonabe.donabe.compile.code.section.GlobalIdentifiersSection;
import io.github.udonabe.donabe.ir.IRProgram;
import java.util.Set;

public final class Compiler {

    public ByteCode compile(IRProgram program, Set<String> globals) {
        ProgramConverter programConverter = new ProgramConverter();

        ByteCode byteCode = programConverter.generate(program);

        GlobalIdentifiersSection identifiers = new GlobalIdentifiersSection(globals);
        byteCode = byteCode.addSection(identifiers);
        
        return byteCode;
    }
}

package io.github.udonabe.donabe.compile;

import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.compile.code.ProgramConverter;
import io.github.udonabe.donabe.compile.code.section.IdentifiersSection;
import io.github.udonabe.donabe.ir.IRProgram;

public final class Compiler {

    public ByteCode compile(IRProgram program, int resolutionMax) {
        ProgramConverter programConverter = new ProgramConverter();

        ByteCode byteCode = programConverter.generate(program);

        IdentifiersSection identifiers = new IdentifiersSection(resolutionMax);
        byteCode = byteCode.addSection(identifiers);
        
        return byteCode;
    }
}

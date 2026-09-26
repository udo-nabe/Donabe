package io.github.udonabe.donabe.compile;

import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.compile.code.ProgramConverter;
import io.github.udonabe.donabe.compile.code.section.GlobalIdentifiersSection;
import io.github.udonabe.donabe.ir.IRProgram;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Compiler {
    private static final Logger log = LoggerFactory.getLogger(Compiler.class);
    
    public ByteCode compile(IRProgram program, Set<String> globals) {
        ProgramConverter programConverter = new ProgramConverter();
        
        ProgramConverter.ProgramConvertResult convertResult = programConverter.generate(program);
        
        log.debug("Generating identifiers section.");
        GlobalIdentifiersSection identifiers = new GlobalIdentifiersSection(globals);
        ByteCode byteCode = new ByteCode(identifiers,
                convertResult.constantPoolSection(),
                convertResult.initializationCodeSection());
        
        log.debug("Generating bytecode successful: {} global identifiers, {} constant pool entries, {} initializer instructions.",
                identifiers.globals().size(),
                convertResult.constantPoolSection().pool().size(),
                convertResult.initializationCodeSection().instructions().size());
        return byteCode;
    }
}

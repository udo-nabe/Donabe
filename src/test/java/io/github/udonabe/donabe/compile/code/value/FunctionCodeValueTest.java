package io.github.udonabe.donabe.compile.code.value;

import io.github.udonabe.donabe.compile.code.instruction.ByteCodeInstruction;
import io.github.udonabe.donabe.compile.code.instruction.OpCode;
import io.github.udonabe.donabe.compile.code.instruction.operand.ConstantPoolOperand;
import io.github.udonabe.donabe.compile.code.section.InitializationCodeSection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FunctionCodeValueTest {

    @Test
    public void testType() {
        assertEquals(new FunctionCodeValue("test", List.of(), Set.of(), new InitializationCodeSection(List.of())).type(),
                CodeValue.FUNCTION_TYPE);
    }

    @Test
    public void testContent() {
        assertArrayEquals(new byte[]{
                    0x04, 0x00, 0x00, 0x00, //関数名の長さ
                    0x74, 0x65, 0x73, 0x74, //関数名
                    0x00, 0x00, //引数スロットの個数(引数無し)
                    0x00, 0x00, //ローカル変数の個数(無し)
                    0x00, 0x00, 0x00, 0x00, //コードの長さ(無し)
                },
                new FunctionCodeValue("test", List.of(), Set.of(), new InitializationCodeSection(List.of())).content());

        Set<Integer> locals = new LinkedHashSet<>();
        locals.add(0);
        locals.add(1);

        assertArrayEquals(new byte[]{
                    0x04, 0x00, 0x00, 0x00, //関数名の長さ
                    0x74, 0x65, 0x73, 0x74, //関数名

                    0x02, 0x00, //引数スロットの個数
                    0x00, 0x00, //引数スロット1
                    0x01, 0x00, //引数スロット2

                    0x02, 0x00, //ローカル変数の個数
                    0x00, 0x00, //ローカル変数1
                    0x01, 0x00, //ローカル変数2

                    0x04, 0x00, 0x00, 0x00, //コードの長さ
                    OpCode.PUSH.opcode(), 0x42, 0x00,
                    OpCode.VRETURN.opcode(),},
                new FunctionCodeValue("test", List.of(0, 1), locals, new InitializationCodeSection(
                        List.of(
                                new ByteCodeInstruction(OpCode.PUSH, List.of(
                                        new ConstantPoolOperand(0x42)
                                )),
                                new ByteCodeInstruction(OpCode.VRETURN, List.of())
                        )
                )).content());
    }
}

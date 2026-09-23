/* Create With GPT-5.6 Luna */
package io.github.udonabe.donabe;

import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.compile.code.constant.ConstantPoolEntry;
import io.github.udonabe.donabe.compile.code.constant.MemberRefEntry;
import io.github.udonabe.donabe.compile.code.constant.TopLevelRefEntry;
import io.github.udonabe.donabe.compile.code.constant.ValueEntry;
import io.github.udonabe.donabe.compile.code.instruction.ByteCodeInstruction;
import io.github.udonabe.donabe.compile.code.instruction.operand.ConstantPoolOperand;
import io.github.udonabe.donabe.compile.code.instruction.operand.DepthOperand;
import io.github.udonabe.donabe.compile.code.instruction.operand.IdentifierSlotOperand;
import io.github.udonabe.donabe.compile.code.instruction.operand.JumpOperand;
import io.github.udonabe.donabe.compile.code.instruction.operand.Operand;
import io.github.udonabe.donabe.compile.code.instruction.operand.SizeOperand;
import io.github.udonabe.donabe.compile.code.section.InitializationCodeSection;
import io.github.udonabe.donabe.compile.code.value.BoolCodeValue;
import io.github.udonabe.donabe.compile.code.value.CodeValue;
import io.github.udonabe.donabe.compile.code.value.FunctionCodeValue;
import io.github.udonabe.donabe.compile.code.value.Int64CodeValue;
import io.github.udonabe.donabe.compile.code.value.IntCodeValue;
import io.github.udonabe.donabe.compile.code.value.ListCodeValue;
import io.github.udonabe.donabe.compile.code.value.StringCodeValue;

import java.util.ArrayList;
import java.util.List;

final class ByteCodeDumper {

    private ByteCodeDumper() {
    }

    static String dump(ByteCode code) {
        StringBuilder out = new StringBuilder();

        dumpGlobals(out, code);
        dumpConstants(out, code);
        dumpFunctions(out, code);
        dumpInitialization(out, code);

        return out.toString();
    }

    private static void dumpGlobals(
            StringBuilder out,
            ByteCode code
    ) {
        out.append("globals\n");

        for (String global : code.globalIdentifiersSection().globals()) {
            out.append("  ")
                    .append(global)
                    .append('\n');
        }

        out.append('\n');
    }

    private static void dumpConstants(
            StringBuilder out,
            ByteCode code
    ) {
        out.append("constants\n");

        List<ConstantPoolEntry<?>> pool =
                code.constantPoolSection().pool();

        for (int i = 0; i < pool.size(); i++) {
            ConstantPoolEntry<?> entry = pool.get(i);

            out.append("  ")
                    .append(i)
                    .append(" = ")
                    .append(dumpConstant(entry))
                    .append('\n');
        }

        out.append('\n');
    }

    private static void dumpFunctions(
            StringBuilder out,
            ByteCode code
    ) {
        for (ConstantPoolEntry<?> entry :
                code.constantPoolSection().pool()) {

            if (!(entry instanceof ValueEntry valueEntry)) {
                continue;
            }

            if (!(valueEntry.value()
                    instanceof FunctionCodeValue function)) {
                continue;
            }

            dumpFunction(out, code, function);
            out.append('\n');
        }
    }

    private static void dumpFunction(
            StringBuilder out,
            ByteCode code,
            FunctionCodeValue function
    ) {
        out.append("function ")
                .append(function.name())
                .append('\n');

        out.append("  params: ");

        if (function.paramSlots().isEmpty()) {
            out.append("[]");
        } else {
            out.append('[');

            for (int i = 0; i < function.paramSlots().size(); i++) {
                if (i > 0) {
                    out.append(", ");
                }

                out.append(function.paramSlots().get(i));
            }

            out.append(']');
        }

        out.append('\n');

        out.append("  locals: ")
                .append(function.localCount())
                .append('\n');

        out.append('\n');

        dumpInstructions(
                out,
                function.code().instructions(),
                code,
                "  "
        );
    }

    private static void dumpInitialization(
            StringBuilder out,
            ByteCode code
    ) {
        out.append("initialization\n");

        dumpInstructions(
                out,
                code.initializationCodeSection().instructions(),
                code,
                "  "
        );
    }

    private static void dumpInstructions(
            StringBuilder out,
            List<ByteCodeInstruction> instructions,
            ByteCode code,
            String indentation
    ) {
        for (int i = 0; i < instructions.size(); i++) {
            ByteCodeInstruction instruction = instructions.get(i);

            out.append(indentation)
                    .append(instruction.opcode());

            for (Operand operand : instruction.operands()) {
                out.append(' ')
                        .append(dumpOperand(operand, code));
            }

            out.append('\n');
        }
    }

    private static String dumpOperand(
            Operand operand,
            ByteCode code
    ) {
        return switch (operand) {
            case ConstantPoolOperand value ->
                    dumpConstantReference(code, value.value());

            case IdentifierSlotOperand value ->
                    Integer.toString(value.value());

            case JumpOperand value ->
                    Integer.toString(value.value());

            case SizeOperand value ->
                    Integer.toString(value.value());
                
            case DepthOperand value -> 
                    Integer.toString(value.value());

            default ->
                    throw new AssertionError(
                            "Unsupported operand: "
                                    + operand.getClass().getName()
                    );
        };
    }

    private static String dumpConstantReference(
            ByteCode code,
            int index
    ) {
        List<ConstantPoolEntry<?>> pool =
                code.constantPoolSection().pool();

        if (index < 0 || index >= pool.size()) {
            throw new AssertionError(
                    "Invalid constant pool index: " + index
            );
        }

        return dumpConstant(pool.get(index));
    }

    private static String dumpConstant(
            ConstantPoolEntry<?> entry
    ) {
        return switch (entry) {
            case ValueEntry value ->
                    dumpValue(value.value());

            case MemberRefEntry value ->
                    "member(" + quote(value.value()) + ")";

            case TopLevelRefEntry value ->
                    "global(" + quote(value.value()) + ")";

            default ->
                    throw new AssertionError(
                            "Unsupported constant pool entry: "
                                    + entry.getClass().getName()
                    );
        };
    }

    private static String dumpValue(CodeValue value) {
        return switch (value) {
            case BoolCodeValue v ->
                    Boolean.toString(v.value());

            case IntCodeValue v ->
                    Integer.toString(v.value());

            case Int64CodeValue v ->
                    Long.toString(v.value()) + "L";

            case StringCodeValue v ->
                    quote(v.value());

            case FunctionCodeValue v ->
                    "function(" + v.name() + ")";

            case ListCodeValue v ->
                    dumpList(v);

            default ->
                    throw new AssertionError(
                            "Unsupported code value: "
                                    + value.getClass().getName()
                    );
        };
    }

    private static String dumpList(ListCodeValue value) {
        StringBuilder out = new StringBuilder();

        out.append('[');

        for (int i = 0; i < value.values().size(); i++) {
            if (i > 0) {
                out.append(", ");
            }

            out.append(dumpValue(value.values().get(i)));
        }

        out.append(']');

        return out.toString();
    }

    private static String quote(String value) {
        return '"' + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + '"';
    }
}
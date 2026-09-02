package io.github.udonabe.donabe.error;

import io.github.udonabe.donabe.ir.instruction.Instruction;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;

import java.util.ArrayList;
import java.util.List;

public class StackTraceGenerator {
    public static String generateStackTrace(StackFrame currentStackFrame) {
        StringBuilder result = new StringBuilder();

        StackFrame current = currentStackFrame;

        List<String> traceLines = new ArrayList<>();
        int mores = 0;
        while (current != null) {
            String frameName = current.name() == null ? "<anonymous>" : current.name();
            Instruction currentInstruction = current.instructions()
                    .get(current.registers().pc());

            StringBuilder buffer = new StringBuilder();
            buffer.append("\tat ").append(frameName);
            buffer.append("(line ").append(currentInstruction.location().line()).append(")");
            buffer.append("\n");

            if (traceLines.size() <= 2) {
                result.append(buffer);
                traceLines.add(buffer.toString());
            } else if (traceLines.stream().allMatch(s -> s.contentEquals(buffer))) {
                mores++;
                traceLines.removeFirst();
                traceLines.add(buffer.toString());
            } else if (mores == 0) {
                result.append(buffer);
                traceLines.removeFirst();
                traceLines.add(buffer.toString());
            } else {
                result.append("\t... ").append(mores).append(" more frames\n");
                mores = 0;

                result.append(buffer);
                traceLines.removeFirst();
                traceLines.add(buffer.toString());
            }

            current = current.caller();
        }

        return result.toString();
    }
}

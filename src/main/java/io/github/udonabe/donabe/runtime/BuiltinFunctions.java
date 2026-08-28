package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.runtime.value.*;

import java.util.ArrayList;
import java.util.List;

public class BuiltinFunctions {
    public static final BuiltinFunctionValue BUILTIN_PRINT = new BuiltinFunctionValue(
            List.of("target"),
            l -> {
                System.out.println(l.getFirst().display());
                return new VoidValue();
            }
    );
    public static final BuiltinFunctionValue BUILTIN_INPUT = new BuiltinFunctionValue(
            List.of(),
            l -> new StringValue(RuntimeIOUtil.RUNTIME_INPUT.nextLine())
    );
    public static final BuiltinFunctionValue BUILTIN_STRING = new BuiltinFunctionValue(
            List.of("target"),
            l -> {
                RuntimeValue<?> value = l.getFirst();
                return new StringValue(value.display());
            }
    );
    public static final BuiltinFunctionValue BUILTIN_LENGTH = new BuiltinFunctionValue(
            List.of("target"),
            l -> {
                RuntimeValue<?> target = l.getFirst();
                return switch (target) {
                    case StringValue(String value) -> new IntegerValue(value.length());
                    case ListValue(List<RuntimeValue<?>> value) -> new IntegerValue(value.size());
                    default -> new IntegerValue(-1);
                };
            }
    );
    public static final BuiltinFunctionValue BUILTIN_RANGE = new BuiltinFunctionValue(
            List.of("start", "end"),
            l -> {
                RuntimeValue<?> s = l.getFirst();
                RuntimeValue<?> e = l.get(1);
                if (s instanceof IntegerValue(Integer start) &&
                    e instanceof IntegerValue(Integer end)) {
                    List<RuntimeValue<?>> res = new ArrayList<>();
                    for (int i = start; i < end; i++) {
                        res.add(new IntegerValue(i));
                    }
                    return new ListValue(res);
                }
                throw new InterpreterException("range()の引数は(int, int)である必要があります。");
            }
    );
    public static final BuiltinFunctionValue BUILTIN_INT = new BuiltinFunctionValue(
            List.of("target"),
            l -> {
                RuntimeValue<?> target = l.getFirst();
                if (target instanceof StringValue(String value)) {
                    try {
                        return new IntegerValue(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {
                        throw new InterpreterException("'" + value + "'を数値に変換できませんでした。");
                    }
                }
                throw new InterpreterException("int()の引数は(string)である必要があります。");
            }
    );
}

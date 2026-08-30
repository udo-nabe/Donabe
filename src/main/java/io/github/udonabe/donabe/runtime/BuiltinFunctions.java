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
}

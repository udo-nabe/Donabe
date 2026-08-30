package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;

import java.util.List;
import java.util.Map;

public sealed interface RuntimeValue<T>
        permits BooleanValue, BuiltinFunctionValue, ClosureValue, FunctionValue,
        IntegerValue, ListValue, StringValue, UndefinedValue, VoidValue {

    T value();
    String typeName();
    String display();
    default RuntimeValue<?> findMember(String name) {
        Map<String, RuntimeValue<?>> anyTypeMembers = Map.of(
                "toString", new BuiltinFunctionValue(
                        List.of(),
                        args -> new StringValue(display())
                )
        );

        if (declaredMembers().containsKey(name)) {
            return declaredMembers().get(name);
        }
        if (anyTypeMembers.containsKey(name)) {
            return anyTypeMembers.get(name);
        }
        throw new InterpreterException("The type '%s' does not have member '%s'.".formatted(typeName(), name));
    }
    Map<String, RuntimeValue<?>> declaredMembers();
}

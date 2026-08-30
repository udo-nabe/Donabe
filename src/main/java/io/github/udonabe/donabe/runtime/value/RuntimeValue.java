package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.value.member.MemberProvider;

import java.util.List;
import java.util.Map;

public sealed interface RuntimeValue<T>
        permits BooleanValue, BuiltinFunctionValue, ClosureValue, FunctionValue,
        IntegerValue, ListValue, StringValue, UndefinedValue, VoidValue {

    T value();
    String typeName();
    String display();
    MemberProvider<?> memberProvider();
    default RuntimeValue<?> findMember(String name) {
        try {
            return memberProvider().findMember(name, this);
        } catch (InterpreterException e) {
            throw new InterpreterException(
                    "The type '%s' does not have member '%s'.".formatted(typeName(), name),
                    e);
        }
    }
}

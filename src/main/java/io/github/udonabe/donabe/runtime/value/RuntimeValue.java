package io.github.udonabe.donabe.runtime.value;

public sealed interface RuntimeValue<T>
        permits BooleanValue, BuiltinFunctionValue, ClosureValue, FunctionValue, IntegerValue, ListValue, StringValue, UndefinedValue, VoidValue {
    T value();
    String typeName();
    String display();
    RuntimeValue<?> getMember(String name);
}

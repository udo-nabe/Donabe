package io.github.udonabe.donabe.runtime.value;

public sealed interface RuntimeValue<T>
        permits BooleanValue, BuiltinFunctionValue, FunctionValue, IntegerValue, ListValue, StringValue, UndefinedValue, VoidValue {
    T value();
    String typeName();
    String display();
}

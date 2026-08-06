package io.github.udonabe.donabe.runtime.value;

public sealed interface RuntimeValue<T>
        permits IntegerValue, StringValue, VoidValue, BooleanValue, FunctionValue, ListValue {
    T value();
    String typeName();
    String display();
}

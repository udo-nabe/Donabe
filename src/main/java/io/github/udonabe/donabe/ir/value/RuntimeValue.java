package io.github.udonabe.donabe.ir.value;

public sealed interface RuntimeValue<T>
        permits BooleanValue, BuiltinFunctionValue, FunctionValue,
        IntegerValue, Int64Value, ListValue, StringValue, UndefinedValue, VoidValue {

    T value();
}

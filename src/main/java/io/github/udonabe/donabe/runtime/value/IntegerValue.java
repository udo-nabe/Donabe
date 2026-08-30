package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.value.member.BooleanMemberProvider;
import io.github.udonabe.donabe.runtime.value.member.IntegerMemberProvider;
import io.github.udonabe.donabe.runtime.value.member.MemberProvider;

import java.util.Map;
import java.util.Objects;

public record IntegerValue(Integer value) implements RuntimeValue<Integer> {
    public IntegerValue {
        Objects.requireNonNull(value);
    }
    @Override
    public String typeName() {
        return "Int";
    }

    @Override
    public String display() {
        return value.toString();
    }

    @Override
    public MemberProvider<?> memberProvider() {
        return new IntegerMemberProvider();
    }
}

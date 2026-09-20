package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.value.member.Int64MemberProvider;
import io.github.udonabe.donabe.runtime.value.member.MemberProvider;

import java.util.Objects;

public record Int64Value(Long value) implements RuntimeValue<Long> {
    public Int64Value {
        Objects.requireNonNull(value);
    }
    @Override
    public String typeName() {
        return "Int64";
    }

    @Override
    public String display() {
        return value.toString();
    }

    @Override
    public MemberProvider<?> memberProvider() {
        return new Int64MemberProvider();
    }
}

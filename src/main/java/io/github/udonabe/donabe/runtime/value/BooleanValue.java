package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.value.member.BooleanMemberProvider;
import io.github.udonabe.donabe.runtime.value.member.MemberProvider;

import java.util.Objects;

public record BooleanValue(Boolean value) implements RuntimeValue<Boolean> {
    public BooleanValue {
        Objects.requireNonNull(value);
    }

    @Override
    public String typeName() {
        return "Bool";
    }

    @Override
    public String display() {
        return value.toString();
    }

    @Override
    public MemberProvider<?> memberProvider() {
        return new BooleanMemberProvider();
    }
}

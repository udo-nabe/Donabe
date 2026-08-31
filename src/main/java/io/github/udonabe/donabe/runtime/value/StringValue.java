package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.value.member.MemberProvider;
import io.github.udonabe.donabe.runtime.value.member.StringMemberProvider;

import java.util.Objects;

public record StringValue(String value) implements RuntimeValue<String> {
    public StringValue {
        Objects.requireNonNull(value);
    }
    @Override
    public String typeName() {
        return "String";
    }

    @Override
    public String display() {
        return value;
    }

    @Override
    public MemberProvider<?> memberProvider() {
        return new StringMemberProvider();
    }
}

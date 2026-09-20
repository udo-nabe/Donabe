package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.Int64Value;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Map;

public class Int64MemberProvider implements MemberProvider<Int64Value> {
    @Override
    public Map<String, RuntimeValue<?>> members(Int64Value receiver) {
        return Map.of();
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.BooleanValue;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Map;

public class BooleanMemberProvider implements MemberProvider<BooleanValue> {
    @Override
    public Map<String, RuntimeValue<?>> members(BooleanValue receiver) {
        return Map.of();
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

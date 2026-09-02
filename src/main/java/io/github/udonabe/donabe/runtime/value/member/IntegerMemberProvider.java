package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.IntegerValue;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Map;

public class IntegerMemberProvider implements MemberProvider<IntegerValue> {
    @Override
    public Map<String, RuntimeValue<?>> members(IntegerValue receiver) {
        return Map.of();
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

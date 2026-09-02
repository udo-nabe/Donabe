package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.RuntimeValue;
import io.github.udonabe.donabe.runtime.value.VoidValue;

import java.util.Map;

public class VoidMemberProvider implements MemberProvider<VoidValue> {
    @Override
    public Map<String, RuntimeValue<?>> members(VoidValue receiver) {
        return Map.of();
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.BooleanValue;
import io.github.udonabe.donabe.runtime.value.ClosureValue;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Map;

public class ClosureMemberProvider implements MemberProvider<ClosureValue> {
    @Override
    public Map<String, RuntimeValue<?>> members(ClosureValue receiver) {
        return Map.of();
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

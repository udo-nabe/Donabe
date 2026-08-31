package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.FunctionValue;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Map;

public class FunctionMemberProvider implements MemberProvider<FunctionValue> {
    @Override
    public Map<String, RuntimeValue<?>> members(FunctionValue receiver) {
        return Map.of();
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.BooleanValue;
import io.github.udonabe.donabe.runtime.value.BuiltinFunctionValue;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Map;

public class BuiltinFunctionMemberProvider implements MemberProvider<BuiltinFunctionValue> {
    @Override
    public Map<String, RuntimeValue<?>> members(BuiltinFunctionValue receiver) {
        return Map.of();
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

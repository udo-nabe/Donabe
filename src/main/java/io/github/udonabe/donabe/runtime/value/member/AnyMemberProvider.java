package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.BuiltinFunctionValue;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;
import io.github.udonabe.donabe.runtime.value.StringValue;

import java.util.List;
import java.util.Map;

public class AnyMemberProvider implements MemberProvider<RuntimeValue<?>> {
    @Override
    public Map<String, RuntimeValue<?>> members(RuntimeValue<?> receiver) {
        return Map.of(
                "toString", new BuiltinFunctionValue(List.of(), args -> new StringValue(receiver.display()))
        );
    }

    @Override
    public MemberProvider<?> parent() {
        return null;
    }
}

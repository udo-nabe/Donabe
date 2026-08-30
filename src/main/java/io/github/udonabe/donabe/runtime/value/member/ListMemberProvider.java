package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.value.ClosureValue;
import io.github.udonabe.donabe.runtime.value.IntegerValue;
import io.github.udonabe.donabe.runtime.value.ListValue;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Map;

public class ListMemberProvider implements MemberProvider<ListValue> {
    @Override
    public Map<String, RuntimeValue<?>> members(ListValue receiver) {
        return Map.of(
                "length", new IntegerValue(receiver.value().size())
        );
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.value.member.BooleanMemberProvider;
import io.github.udonabe.donabe.runtime.value.member.MemberProvider;
import io.github.udonabe.donabe.runtime.value.member.VoidMemberProvider;

import java.util.Map;

public final class VoidValue implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<void>";
    }

    @Override
    public String typeName() {
        return "Void";
    }

    @Override
    public String display() {
        return "<void>";
    }

    @Override
    public boolean equals(Object obj) {
        return true;
    }

    @Override
    public MemberProvider<?> memberProvider() {
        return new VoidMemberProvider();
    }
}

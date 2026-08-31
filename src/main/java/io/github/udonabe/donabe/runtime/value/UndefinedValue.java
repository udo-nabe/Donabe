package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.runtime.context.stack.StackFrame;
import io.github.udonabe.donabe.runtime.value.member.MemberProvider;

/**
 * この値に対して行う操作は全て意味解析器で弾くべきであるため、
 * いかなる操作でもIllegalStateExceptionが送出される。
 */
public record UndefinedValue() implements RuntimeValue<Void> {
    @Override
    public Void value() {
        throw new IllegalStateException("Undefined identifier.");
    }

    @Override
    public String typeName() {
        throw new IllegalStateException("Undefined identifier.");
    }

    @Override
    public String display() {
        throw new IllegalStateException("Undefined identifier.");
    }

    @Override
    public RuntimeValue<?> findMember(String memberName, StackFrame currentFrame) {
        throw new IllegalStateException("Undefined identifier.");
    }

    @Override
    public MemberProvider<?> memberProvider() {
        throw new IllegalStateException("Undefined identifier.");
    }
}

package io.github.udonabe.donabe.runtime.value;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.runtime.InterpreterException;

import java.util.Map;

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
    public RuntimeValue<?> findMember(String memberName) {
        throw new IllegalStateException("Undefined identifier.");
    }

    @Override
    public Map<String, RuntimeValue<?>> declaredMembers() {
        return Map.of();
    }
}

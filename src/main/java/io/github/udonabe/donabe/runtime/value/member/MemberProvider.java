package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.value.RuntimeValue;

import java.util.Map;

public interface MemberProvider<T extends RuntimeValue<?>> {
    Map<String, RuntimeValue<?>> members(T receiver);
    MemberProvider<?> parent();

    @SuppressWarnings("unchecked")
    default RuntimeValue<?> findMember(String name, RuntimeValue<?> receiver) {
        var members = this.members((T) receiver);
        if (members.containsKey(name)) {
            return members.get(name);
        }
        if (parent() == null) {
            throw new InterpreterException("Member '%s' not found.".formatted(name), null);
        }
        return parent().findMember(name, receiver);
    }
}

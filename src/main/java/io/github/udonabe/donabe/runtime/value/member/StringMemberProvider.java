package io.github.udonabe.donabe.runtime.value.member;

import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.value.*;

import java.util.List;
import java.util.Map;

public class StringMemberProvider implements MemberProvider<StringValue> {
    @Override
    public Map<String, RuntimeValue<?>> members(StringValue receiver) {
        return Map.of(
                "length", new IntegerValue(receiver.value().length()),
                "toInt", new BuiltinFunctionValue(
                        List.of(),
                        args -> {
                            try {
                                return new IntegerValue(Integer.parseInt(receiver.value()));
                            } catch (NumberFormatException e) {
                                throw new InterpreterException("Could not convert String to Int.", e);
                            }
                        }
                )
        );
    }

    @Override
    public MemberProvider<RuntimeValue<?>> parent() {
        return new AnyMemberProvider();
    }
}

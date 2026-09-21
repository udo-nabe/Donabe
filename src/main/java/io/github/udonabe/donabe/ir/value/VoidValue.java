package io.github.udonabe.donabe.ir.value;


public final class VoidValue implements RuntimeValue<String> {
    @Override
    public String value() {
        return "<void>";
    }
}

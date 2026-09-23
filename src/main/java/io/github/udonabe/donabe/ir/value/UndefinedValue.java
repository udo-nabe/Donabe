package io.github.udonabe.donabe.ir.value;

/**
 * この値に対して行う操作は全て意味解析器で弾くべきであるため、
 * いかなる操作でもIllegalStateExceptionが送出される。
 */
public record UndefinedValue() implements RuntimeValue<Void> {
    @Override
    public Void value() {
        throw new IllegalStateException("Undefined identifier.");
    }
}

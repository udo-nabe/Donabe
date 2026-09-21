package io.github.udonabe.donabe.ir.value;

import java.util.ArrayList;
import java.util.List;

public record ListValue(List<RuntimeValue<?>> value) implements RuntimeValue<List<RuntimeValue<?>>> {
}

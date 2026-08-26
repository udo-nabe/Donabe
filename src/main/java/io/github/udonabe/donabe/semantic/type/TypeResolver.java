package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.ast.type.FunctionTypeAnnotation;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;
import io.github.udonabe.donabe.semantic.type.builtin.*;
import io.github.udonabe.donabe.semantic.type.function.FunctionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

public class TypeResolver {
    private final Map<String, Type> namedTypeMap;
    private final String source;

    public TypeResolver(String source) {
        this.source = source;
        this.namedTypeMap = Map.ofEntries(
                entry("Int", new IntType()),
                entry("Bool", new BooleanType()),
                entry("String", new StringType()),
                entry("List", new ListType()),
                entry("Void", new VoidType())
        );
    }

    public Type resolve(TypeAnnotation annotation) {
        return switch (annotation) {
            case FunctionTypeAnnotation functionTypeAnnotation -> resolveFunction(functionTypeAnnotation);
            case NamedTypeAnnotation namedTypeAnnotation -> resolveName(namedTypeAnnotation);
        };
    }

    private Type resolveName(NamedTypeAnnotation annotation) {
        String type = annotation.name().name();
        if (!namedTypeMap.containsKey(type)) {
            throw new CompileException(ErrorUtil.makeError(annotation.location(), source, "Type '%s' is not defined.", type));
        }
        return namedTypeMap.get(type);
    }

    private Type resolveFunction(FunctionTypeAnnotation annotation) {
        List<Type> paramTypes = annotation.params().stream()
                .map(this::resolve)
                .toList();
        Type returnType = resolve(annotation.returnType());
        return new FunctionType(paramTypes, returnType);
    }
}

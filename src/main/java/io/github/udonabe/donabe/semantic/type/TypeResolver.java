package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.ast.type.*;
import io.github.udonabe.donabe.runtime.InterpreterException;
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
                entry("List", new ListType(new AnyType())),
                entry("Void", new VoidType()),
                entry("Any", new AnyType())
        );
    }

    public Type resolve(TypeAnnotation annotation) {
        return switch (annotation) {
            case FunctionTypeAnnotation functionTypeAnnotation -> resolveFunction(functionTypeAnnotation);
            case NamedTypeAnnotation namedTypeAnnotation -> resolveName(namedTypeAnnotation);
            case GenericTypeAnnotation genericTypeAnnotation -> resolveGenericType(genericTypeAnnotation);
            case UnknownTypeAnnotation ignored -> throw new IllegalStateException("UnknownTypeAnnotation has not been inferred. location=" + annotation.location());
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

    private Type resolveGenericType(GenericTypeAnnotation annotation) {
        Type baseType = resolve(annotation.baseType());
        List<Type> paramTypes = annotation.typeParameters().stream()
                .map(this::resolve)
                .toList();

        //暫定的にList型のパラメータのみ対応する
        if (baseType instanceof ListType) {
            if (paramTypes.size() != 1) {
                throw new InterpreterException(ErrorUtil.makeError(annotation.location(), source,
                        "The 'List' type requires a single type parameter."));
            }
            return new ListType(paramTypes.getFirst());
        }
        throw new UnsupportedOperationException("Type parameters other than the 'List' type are currently not supported.");
    }
}

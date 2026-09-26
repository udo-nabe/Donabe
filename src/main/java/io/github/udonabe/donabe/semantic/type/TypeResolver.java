package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.error.ErrorUtil;
import io.github.udonabe.donabe.ast.type.*;
import io.github.udonabe.donabe.semantic.type.builtin.*;
import io.github.udonabe.donabe.semantic.type.function.FunctionType;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeResolver {

    private static final Logger log = LoggerFactory.getLogger(TypeResolver.class);
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
        log.trace("Resolving type annotation: {}", annotation.typeString());

        var result = switch (annotation) {
            case FunctionTypeAnnotation functionTypeAnnotation ->
                resolveFunction(functionTypeAnnotation);
            case NamedTypeAnnotation namedTypeAnnotation ->
                resolveName(namedTypeAnnotation);
            case GenericTypeAnnotation genericTypeAnnotation ->
                resolveGenericType(genericTypeAnnotation);
            case UnknownTypeAnnotation ignored ->
                throw new IllegalStateException("UnknownTypeAnnotation has not been inferred. location=" + annotation.location());
        };
        log.trace("Resolved: {}", result);
        return result;
    }

    private Type resolveName(NamedTypeAnnotation annotation) {
        String type = annotation.name().name();
        if (!namedTypeMap.containsKey(type)) {
            throw new CompileException(ErrorUtil.makeError(annotation.location(), source, "Type '%s' is not defined.", type));
        }

        Type result = namedTypeMap.get(type);
        return result;
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
                throw new CompileException(ErrorUtil.makeError(annotation.location(), source,
                        "The 'List' type requires a single type parameter."));
            }
            return new ListType(paramTypes.getFirst());
        }
        throw new UnsupportedOperationException("Type parameters other than the 'List' type are currently not supported.");
    }
}

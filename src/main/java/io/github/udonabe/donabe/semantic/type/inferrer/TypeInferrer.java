package io.github.udonabe.donabe.semantic.type.inferrer;

import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;
import io.github.udonabe.donabe.ast.type.UnknownTypeAnnotation;
import io.github.udonabe.donabe.semantic.type.Type;
import io.github.udonabe.donabe.semantic.type.TypeResolver;

public class TypeInferrer {
    private final TypeResolver typeResolver;
    private final String source;

    public TypeInferrer(String source) {
        this.source = source;
        this.typeResolver = new TypeResolver(source);
    }

    public Type inferVariableDeclaration(TypeAnnotation declarationType, Type initializerType) {
        if (!(declarationType instanceof UnknownTypeAnnotation)) {
            return typeResolver.resolve(declarationType);
        }
        return initializerType;
    }
}

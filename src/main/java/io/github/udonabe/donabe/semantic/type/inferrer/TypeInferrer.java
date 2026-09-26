package io.github.udonabe.donabe.semantic.type.inferrer;

import io.github.udonabe.donabe.ast.type.TypeAnnotation;
import io.github.udonabe.donabe.ast.type.UnknownTypeAnnotation;
import io.github.udonabe.donabe.semantic.type.Type;
import io.github.udonabe.donabe.semantic.type.TypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeInferrer {
    private static final Logger log = LoggerFactory.getLogger(TypeInferrer.class);
    private final TypeResolver typeResolver;
    private final String source;

    public TypeInferrer(String source) {
        this.source = source;
        this.typeResolver = new TypeResolver(source);
    }

    public Type inferVariableDeclaration(TypeAnnotation declarationType, Type initializerType) {
        log.trace("Inferring variable declaration. Type annotation: {}", declarationType);
        
        if (!(declarationType instanceof UnknownTypeAnnotation)) {
            log.trace("Skipped inferring.");
            return typeResolver.resolve(declarationType);
        }
        
        log.trace("Inferred: initializer={}", initializerType);
        return initializerType;
    }
}

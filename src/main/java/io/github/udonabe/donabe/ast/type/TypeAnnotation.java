package io.github.udonabe.donabe.ast.type;

import io.github.udonabe.donabe.ast.ASTNode;

public sealed interface TypeAnnotation extends ASTNode
        permits FunctionTypeAnnotation, GenericTypeAnnotation, NamedTypeAnnotation, UnknownTypeAnnotation {
    String typeString();
}

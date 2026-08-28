package io.github.udonabe.donabe.ast.type;

import io.github.udonabe.donabe.ast.ASTNode;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

public sealed interface TypeAnnotation extends ASTNode
        permits FunctionTypeAnnotation, GenericTypeAnnotation, NamedTypeAnnotation, UnknownTypeAnnotation {
    String typeString();
}

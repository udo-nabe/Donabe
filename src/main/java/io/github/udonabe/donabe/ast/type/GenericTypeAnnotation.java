package io.github.udonabe.donabe.ast.type;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

import java.util.List;
import java.util.stream.Collectors;

public record GenericTypeAnnotation(TypeAnnotation baseType, List<TypeAnnotation> typeParameters) implements TypeAnnotation {
    @Override
    public SourceFileLocation location() {
        return baseType.location();
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitGenericTypeAnnotation(this);
    }

    @Override
    public String typeString() {
        String parameters = typeParameters.stream()
                .map(TypeAnnotation::typeString)
                .collect(Collectors.joining(", ", "<", ">"));
        return baseType.typeString() + parameters;
    }
}

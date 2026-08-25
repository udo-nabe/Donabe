package io.github.udonabe.donabe.ast.type;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;

import java.util.List;
import java.util.stream.Collectors;

public record FunctionTypeAnnotation(
        List<TypeAnnotation> params,
        TypeAnnotation returnType,
        SourceFileLocation location) implements TypeAnnotation {
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFunctionTypeAnnotation(this);
    }

    @Override
    public String typeString() {
        String paramString = params.stream()
                .map(TypeAnnotation::typeString)
                .collect(Collectors.joining(", ", "(", ")"));
        return paramString + " -> " + returnType.typeString();
    }
}

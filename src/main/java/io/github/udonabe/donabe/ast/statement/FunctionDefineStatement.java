package io.github.udonabe.donabe.ast.statement;

import java.util.List;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.type.FunctionTypeAnnotation;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;

public record FunctionDefineStatement(Identifier name, List<Parameter> params, TypeAnnotation returnType,
        BlockStatement block, SourceFileLocation location) implements Statement, Definition {

    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFunctionDefineStatement(this);
    }

    @Override
    public TypeAnnotation type() {
        return new FunctionTypeAnnotation(params.stream()
                .map(p -> p.type())
                .toList(),
                returnType,
                location);
    }
}

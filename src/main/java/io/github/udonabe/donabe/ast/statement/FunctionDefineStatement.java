package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;

import java.util.List;

public record FunctionDefineStatement(Identifier identifier, List<Parameter> params, TypeAnnotation returnType,
                                      BlockStatement block, SourceFileLocation location) implements Statement {


    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFunctionDefineStatement(this);
    }
}

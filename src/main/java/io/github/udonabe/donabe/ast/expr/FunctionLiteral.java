package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.statement.BlockStatement;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;

import java.util.List;

public record FunctionLiteral(List<Parameter> args, TypeAnnotation type, BlockStatement block,
                              SourceFileLocation location) implements Expression {


    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFunctionLiteral(this);
    }

    @Override
    public String display() {
        return type.typeString();
    }
}

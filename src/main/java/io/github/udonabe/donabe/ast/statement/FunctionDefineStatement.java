package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Expression;
import io.github.udonabe.donabe.ast.expr.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record FunctionDefineStatement(Identifier identifier, List<Identifier> args, BlockStatement block,
                                      SourceFileLocation location) implements Statement {


    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
        return visitor.visitFunctionDefineStatement(this);
    }
}

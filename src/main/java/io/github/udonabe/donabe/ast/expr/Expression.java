package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTNode;

public sealed interface Expression extends ASTNode
        permits IntegerLiteral, Identifier, BinaryExpression, StringLiteral,
        CallExpression, BooleanLiteral, UnaryExpression, AssignExpression,
        Increment, Decrement, FunctionLiteral, VoidExpression, ListLiteral, IndexExpression {
    String display();
}

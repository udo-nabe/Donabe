package io.github.udonabe.donabe.ast.expr;

import io.github.udonabe.donabe.ast.ASTNode;

public sealed interface Expression extends ASTNode
        permits AssignExpression, BinaryExpression, BooleanLiteral, CallExpression, CompoundAssignExpression, Decrement, FunctionLiteral, Identifier, Increment, IndexExpression, IntegerLiteral, ListLiteral, MemberAccessExpression, StringLiteral, UnaryExpression, VoidExpression {
    String display();
}

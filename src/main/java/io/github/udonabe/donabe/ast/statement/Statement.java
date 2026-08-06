package io.github.udonabe.donabe.ast.statement;

import io.github.udonabe.donabe.ast.ASTNode;

public sealed interface Statement extends ASTNode
        permits LetDeclaration, ExpressionStatement, BlockStatement, IfStatement, WhileStatement, VarDeclaration, ReturnStatement, EmptyStatement, FunctionDefineStatement, ForEachStatement {
}

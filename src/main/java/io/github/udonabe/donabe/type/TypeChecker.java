package io.github.udonabe.donabe.type;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;

public class TypeChecker implements ASTVisitor<Type> {

    @Override
    public Type visitProgram(Program program) {
        return null;
    }

    @Override
    public Type visitBlockStatement(BlockStatement statement) {
        return null;
    }

    @Override
    public Type visitEmptyStatement(EmptyStatement statement) {
        return null;
    }

    @Override
    public Type visitExpressionStatement(ExpressionStatement statement) {
        return null;
    }

    @Override
    public Type visitFunctionDefineStatement(FunctionDefineStatement statement) {
        return null;
    }

    @Override
    public Type visitIfStatement(IfStatement statement) {
        return null;
    }

    @Override
    public Type visitLetDeclaration(LetDeclaration statement) {
        return null;
    }

    @Override
    public Type visitReturnStatement(ReturnStatement statement) {
        return null;
    }

    @Override
    public Type visitVarDeclaration(VarDeclaration statement) {
        return null;
    }

    @Override
    public Type visitWhileStatement(WhileStatement statement) {
        return null;
    }

    @Override
    public Type visitForEachStatement(ForEachStatement statement) {
        return null;
    }

    @Override
    public Type visitAssignExpression(AssignExpression expr) {
        return null;
    }

    @Override
    public Type visitBinaryExpression(BinaryExpression expr) {
        return null;
    }

    @Override
    public Type visitBooleanLiteral(BooleanLiteral expr) {
        return null;
    }

    @Override
    public Type visitCallExpression(CallExpression expr) {
        return null;
    }

    @Override
    public Type visitCompoundAssignExpression(CompoundAssignExpression expr) {
        return null;
    }

    @Override
    public Type visitDecrement(Decrement expr) {
        return null;
    }

    @Override
    public Type visitFunctionLiteral(FunctionLiteral expr) {
        return null;
    }

    @Override
    public Type visitIdentifier(Identifier expr) {
        return null;
    }

    @Override
    public Type visitIncrement(Increment expr) {
        return null;
    }

    @Override
    public Type visitIndexExpression(IndexExpression expr) {
        return null;
    }

    @Override
    public Type visitIntegerLiteral(IntegerLiteral expr) {
        return null;
    }

    @Override
    public Type visitListLiteral(ListLiteral expr) {
        return null;
    }

    @Override
    public Type visitStringLiteral(StringLiteral expr) {
        return null;
    }

    @Override
    public Type visitUnaryExpression(UnaryExpression expr) {
        return null;
    }

    @Override
    public Type visitVoidExpression(VoidExpression expr) {
        return null;
    }
}

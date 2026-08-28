package io.github.udonabe.donabe.ast;

import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ast.type.FunctionTypeAnnotation;
import io.github.udonabe.donabe.ast.type.GenericTypeAnnotation;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;

public interface ASTVisitor<R> {
    R visitProgram(Program program);

    // Statements
    R visitBlockStatement(BlockStatement statement);
    R visitEmptyStatement(EmptyStatement statement);
    R visitExpressionStatement(ExpressionStatement statement);
    R visitFunctionDefineStatement(FunctionDefineStatement statement);
    R visitIfStatement(IfStatement statement);
    R visitLetDeclaration(LetDeclaration statement);
    R visitReturnStatement(ReturnStatement statement);
    R visitVarDeclaration(VarDeclaration statement);
    R visitWhileStatement(WhileStatement statement);
    R visitForEachStatement(ForEachStatement statement);

    // Expressions
    R visitAssignExpression(AssignExpression expr);
    R visitBinaryExpression(BinaryExpression expr);
    R visitBooleanLiteral(BooleanLiteral expr);
    R visitCallExpression(CallExpression expr);
    R visitCompoundAssignExpression(CompoundAssignExpression expr);
    R visitDecrement(Decrement expr);
    R visitFunctionLiteral(FunctionLiteral expr);
    R visitIdentifier(Identifier expr);
    R visitIncrement(Increment expr);
    R visitIndexExpression(IndexExpression expr);
    R visitIntegerLiteral(IntegerLiteral expr);
    R visitListLiteral(ListLiteral expr);
    R visitStringLiteral(StringLiteral expr);
    R visitUnaryExpression(UnaryExpression expr);
    R visitVoidExpression(VoidExpression expr);

    //Type Annotation
    R visitNamedTypeAnnotation(NamedTypeAnnotation typeAnnotation);
    R visitFunctionTypeAnnotation(FunctionTypeAnnotation typeAnnotation);
    R visitGenericTypeAnnotation(GenericTypeAnnotation typeAnnotation);

    //Parameter
    R visitParameter(Parameter parameter);
}

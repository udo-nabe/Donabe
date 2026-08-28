package io.github.udonabe.donabe.semantic.flow;

import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ast.type.FunctionTypeAnnotation;
import io.github.udonabe.donabe.ast.type.GenericTypeAnnotation;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;

public class FlowAnalyzer implements ASTVisitor<FlowInfo> {
    public FlowAnalyzer() {
    }

    @Override
    public FlowInfo visitProgram(Program program) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitBlockStatement(BlockStatement statement) {
        return new FlowInfo(
                statement.statements().stream()
                        .allMatch(s -> s.accept(this).canFallThrough())
        );
    }

    @Override
    public FlowInfo visitEmptyStatement(EmptyStatement statement) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitExpressionStatement(ExpressionStatement statement) {
        return statement.expression().accept(this);
    }

    @Override
    public FlowInfo visitFunctionDefineStatement(FunctionDefineStatement statement) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitIfStatement(IfStatement statement) {
        if (statement.elseBlock() == null) {
            return new FlowInfo(true);
        }
        FlowInfo thenBlockInfo = statement.thenBlock().accept(this);
        FlowInfo elseBlockInfo = statement.elseBlock().accept(this);

        return new FlowInfo(
                thenBlockInfo.canFallThrough() || elseBlockInfo.canFallThrough()
        );
    }

    @Override
    public FlowInfo visitLetDeclaration(LetDeclaration statement) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitReturnStatement(ReturnStatement statement) {
        return new FlowInfo(false);
    }

    @Override
    public FlowInfo visitVarDeclaration(VarDeclaration statement) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitWhileStatement(WhileStatement statement) {
        return new FlowInfo(true);  //フロー解析を単純化するため、暫定的にtrueにする
    }

    @Override
    public FlowInfo visitForEachStatement(ForEachStatement statement) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public FlowInfo visitAssignExpression(AssignExpression expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitBinaryExpression(BinaryExpression expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitBooleanLiteral(BooleanLiteral expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitCallExpression(CallExpression expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitCompoundAssignExpression(CompoundAssignExpression expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitDecrement(Decrement expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitFunctionLiteral(FunctionLiteral expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitIdentifier(Identifier expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitIncrement(Increment expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitIndexExpression(IndexExpression expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitIntegerLiteral(IntegerLiteral expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitListLiteral(ListLiteral expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitStringLiteral(StringLiteral expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitUnaryExpression(UnaryExpression expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitVoidExpression(VoidExpression expr) {
        return new FlowInfo(true);
    }

    @Override
    public FlowInfo visitNamedTypeAnnotation(NamedTypeAnnotation typeAnnotation) {
        throw new AssertionError();
    }

    @Override
    public FlowInfo visitFunctionTypeAnnotation(FunctionTypeAnnotation typeAnnotation) {
        throw new AssertionError();
    }

    @Override
    public FlowInfo visitGenericTypeAnnotation(GenericTypeAnnotation typeAnnotation) {
        throw new AssertionError();
    }

    @Override
    public FlowInfo visitParameter(Parameter parameter) {
        throw new AssertionError();
    }
}

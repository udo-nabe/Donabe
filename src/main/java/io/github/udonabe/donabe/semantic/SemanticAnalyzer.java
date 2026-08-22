package io.github.udonabe.donabe.semantic;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ir.IRProgram;
import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.RuntimeIOUtil;
import io.github.udonabe.donabe.runtime.VariableCell;
import io.github.udonabe.donabe.runtime.value.*;
import io.github.udonabe.donabe.semantic.ir.IRGenerator;
import io.github.udonabe.donabe.semantic.resolve.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class SemanticAnalyzer implements ASTVisitor<SymbolInformation> {
    private static final Logger log = LoggerFactory.getLogger(SemanticAnalyzer.class);
    private final String source;
    private Scope currentScope;
    private final AnalyzeContext context;
    private Map<Integer, VariableCell> resolution;

    public SemanticAnalyzer(String source) {
        this.source = source;
        this.context = new AnalyzeContext();
    }

    private int nextId() {
        return resolution.size();
    }

    public AnalyzeResult check(Program program) {
        NameResolver.ResolveResult resolveResult = new NameResolver(source).resolve(program);

        Scope rootScope = resolveResult.root();
        this.currentScope = rootScope;
        this.resolution = resolveResult.resolution();

        program.accept(this);

        rootScope.resetChildPos();
        IRProgram ir = new IRGenerator(rootScope, resolveResult.localsASTNodeMap()).generate(program);

        return new AnalyzeResult(ir, resolution);
    }

    private void checkFunctions(List<FunctionDefineStatement> functionDefineStatements) {
        for (FunctionDefineStatement s : functionDefineStatements) {
            context.pushFunction();
            s.block().accept(this);
            context.popFunction();
        }
    }

    @Override
    public SymbolInformation visitProgram(Program program) {
        List<Statement> statements = program.statements();

        List<FunctionDefineStatement> defines = statements.stream()
                .filter(s -> s instanceof FunctionDefineStatement)
                .map(s -> (FunctionDefineStatement) s)
                .toList();
        checkFunctions(defines);

        for (Statement statement : statements) {
            if (statement == null) continue;
            statement.accept(this);
        }
        return null;
    }

    @Override
    public SymbolInformation visitBlockStatement(BlockStatement statement) {
        currentScope = currentScope.nextChildScope();

        List<FunctionDefineStatement> defines = statement.statements().stream()
                .filter(s -> s instanceof FunctionDefineStatement)
                .map(s -> (FunctionDefineStatement) s)
                .toList();
        checkFunctions(defines);

        for (Statement s : statement.statements()) {
            s.accept(this);
        }
        currentScope = currentScope.parent();

        return null;
    }

    @Override
    public SymbolInformation visitEmptyStatement(EmptyStatement statement) {
        return null;
    }

    @Override
    public SymbolInformation visitExpressionStatement(ExpressionStatement statement) {
        statement.expression().accept(this);
        return null;
    }

    @Override
    public SymbolInformation visitFunctionDefineStatement(FunctionDefineStatement statement) {
        log.trace("Skipped FunctionDefineStatement: {}", statement);
        return null;
    }

    @Override
    public SymbolInformation visitIfStatement(IfStatement statement) {
        statement.condition().accept(this);
        statement.thenBlock().accept(this);
        statement.elseBlock().accept(this);
        return null;
    }

    @Override
    public SymbolInformation visitLetDeclaration(LetDeclaration statement) {
        statement.expr().accept(this);
        return null;
    }

    @Override
    public SymbolInformation visitReturnStatement(ReturnStatement statement) {
        if (!context.inFunction()) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source, "return文は関数の外で使用できません。"));
        }
        return null;
    }

    @Override
    public SymbolInformation visitVarDeclaration(VarDeclaration statement) {
        statement.expr().accept(this);
        return null;
    }

    @Override
    public SymbolInformation visitWhileStatement(WhileStatement statement) {
        statement.condition().accept(this);
        statement.loop().accept(this);
        return null;
    }

    @Override
    public SymbolInformation visitForEachStatement(ForEachStatement statement) {
        statement.iterable().accept(this);
        currentScope = currentScope.nextChildScope();
        statement.body().accept(this);
        currentScope = currentScope.parent();
        return null;
    }

    @Override
    public SymbolInformation visitAssignExpression(AssignExpression expr) {
        var target = expr.target().accept(this);
        if (!target.isAssignable()) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source, "式\"%s\"へは代入できません。", expr.target().display()));
        }

        return target;
    }

    @Override
    public SymbolInformation visitBinaryExpression(BinaryExpression expr) {
        expr.left().accept(this);
        expr.right().accept(this);
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitBooleanLiteral(BooleanLiteral expr) {
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitCallExpression(CallExpression expr) {
        Expression callee = expr.target();
        callee.accept(this);
        for (Expression arg : expr.args()) {
            arg.accept(this);
        }
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitCompoundAssignExpression(CompoundAssignExpression expr) {
        var result = expr.target().accept(this);
        expr.value().accept(this);
        return result;
    }

    @Override
    public SymbolInformation visitDecrement(Decrement expr) {
        expr.target().accept(this);
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitFunctionLiteral(FunctionLiteral expr) {
        context.pushFunction();
        expr.block().accept(this);
        context.popFunction();
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitIdentifier(Identifier expr) {
        return currentScope.get(expr.name());
    }

    @Override
    public SymbolInformation visitIncrement(Increment expr) {
        expr.target().accept(this);
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitIndexExpression(IndexExpression expr) {
        expr.target().accept(this);
        expr.index().accept(this);
        return new SymbolInformation(true);
    }

    @Override
    public SymbolInformation visitIntegerLiteral(IntegerLiteral expr) {
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitListLiteral(ListLiteral expr) {
        for (Expression e : expr.elements()) {
            e.accept(this);
        }
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitStringLiteral(StringLiteral expr) {
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitUnaryExpression(UnaryExpression expr) {
        expr.expr().accept(this);
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitVoidExpression(VoidExpression expr) {
        return new SymbolInformation(false);
    }

    public record AnalyzeResult(IRProgram irProgram, Map<Integer, VariableCell> resolution) {}
}

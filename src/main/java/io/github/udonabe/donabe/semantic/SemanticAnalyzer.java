package io.github.udonabe.donabe.semantic;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.AssignExpression;
import io.github.udonabe.donabe.ast.expr.BinaryExpression;
import io.github.udonabe.donabe.ast.expr.BooleanLiteral;
import io.github.udonabe.donabe.ast.expr.CallExpression;
import io.github.udonabe.donabe.ast.expr.CompoundAssignExpression;
import io.github.udonabe.donabe.ast.expr.Decrement;
import io.github.udonabe.donabe.ast.expr.Expression;
import io.github.udonabe.donabe.ast.expr.FunctionLiteral;
import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.expr.Increment;
import io.github.udonabe.donabe.ast.expr.IndexExpression;
import io.github.udonabe.donabe.ast.expr.IntegerLiteral;
import io.github.udonabe.donabe.ast.expr.ListLiteral;
import io.github.udonabe.donabe.ast.expr.MemberAccessExpression;
import io.github.udonabe.donabe.ast.expr.StringLiteral;
import io.github.udonabe.donabe.ast.expr.UnaryExpression;
import io.github.udonabe.donabe.ast.expr.VoidExpression;
import io.github.udonabe.donabe.ast.statement.BlockStatement;
import io.github.udonabe.donabe.ast.statement.Definition;
import io.github.udonabe.donabe.ast.statement.EmptyStatement;
import io.github.udonabe.donabe.ast.statement.ExpressionStatement;
import io.github.udonabe.donabe.ast.statement.ForEachStatement;
import io.github.udonabe.donabe.ast.statement.FunctionDefineStatement;
import io.github.udonabe.donabe.ast.statement.IfStatement;
import io.github.udonabe.donabe.ast.statement.LetDeclaration;
import io.github.udonabe.donabe.ast.statement.ReturnStatement;
import io.github.udonabe.donabe.ast.statement.Statement;
import io.github.udonabe.donabe.ast.statement.VarDeclaration;
import io.github.udonabe.donabe.ast.statement.WhileStatement;
import io.github.udonabe.donabe.ast.type.FunctionTypeAnnotation;
import io.github.udonabe.donabe.ast.type.GenericTypeAnnotation;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;
import io.github.udonabe.donabe.error.ErrorUtil;
import io.github.udonabe.donabe.ir.IRProgram;
import io.github.udonabe.donabe.semantic.ir.IRGenerator;
import io.github.udonabe.donabe.semantic.resolve.NameResolver;
import io.github.udonabe.donabe.semantic.type.TypeChecker;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class SemanticAnalyzer implements ASTVisitor<SymbolInformation> {

    private static final Logger log = LoggerFactory.getLogger(SemanticAnalyzer.class);
    private final String source;
    private Scope currentScope;
    private final Map<String, SymbolInformation> globals;
    private final AnalyzeContext context;

    public SemanticAnalyzer(String source) {
        this.source = source;
        this.context = new AnalyzeContext();
        this.globals = new HashMap<>();
    }

    public AnalyzeResult check(Program program) {
        NameResolver.ResolveResult resolveResult = new NameResolver(source).resolve(program);

        Scope rootScope = resolveResult.root();
        this.currentScope = rootScope;

        program.accept(this);

        new TypeChecker(source, resolveResult.resolutionMap()).check(program);

        IRProgram ir = new IRGenerator(resolveResult.resolutionMap(), resolveResult.globals(), resolveResult.localCountASTNodeMap())
                .generate(program);

        return new AnalyzeResult(ir, resolveResult.globals()
                .stream()
                .map(s -> s.fullyQualifiedName()).collect(Collectors.toSet()));
    }

    private void checkFunction(FunctionDefineStatement statement) {
        context.pushFunction();
        statement.block().accept(this);
        context.popFunction();
        globals.put(statement.name().name(), new SymbolInformation(false));
    }

    private void checkGlobals(List<Definition> functionDefineStatements) {
        for (Definition definition : functionDefineStatements) {
            if (definition instanceof FunctionDefineStatement functionDefine) {
                checkFunction(functionDefine);
            } else {
                definition.accept(this);
            }
        }
    }

    @Override
    public SymbolInformation visitProgram(Program program) {
        List<Definition> statements = program.definitions();

        checkGlobals(statements);

        for (Statement statement : statements) {
            if (statement == null) {
                continue;
            }
            statement.accept(this);
        }
        return null;
    }

    @Override
    public SymbolInformation visitBlockStatement(BlockStatement statement) {
        currentScope = currentScope.nextChildScope();

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
        if (context.inFunction()) {
            checkFunction(statement);
        } else {
            log.trace("Skipped FunctionDefineStatement: {}", statement);
        }
        return null;
    }

    @Override
    public SymbolInformation visitIfStatement(IfStatement statement) {
        statement.condition().accept(this);
        statement.thenBlock().accept(this);
        if (statement.elseBlock() != null) {
            statement.elseBlock().accept(this);
        }
        return null;
    }

    @Override
    public SymbolInformation visitLetDeclaration(LetDeclaration statement) {
        statement.expr().accept(this);
        if (!context.inFunction()) {
            globals.put(statement.name().name(), new SymbolInformation(false));
        }
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
        if (!context.inFunction()) {
            globals.put(statement.name().name(), new SymbolInformation(true));
        }
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
            throw new CompileException(
                    ErrorUtil.makeError(expr.location(), source, "式\"%s\"へは代入できません。", expr.target().display()));
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
        SymbolInformation local = currentScope.get(expr.name());
        if (local != null) {
            return local;
        }
        return globals.get(expr.name());
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
    public SymbolInformation visitMemberAccessExpression(MemberAccessExpression expr) {
        return null;
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

    @Override
    public SymbolInformation visitNamedTypeAnnotation(NamedTypeAnnotation typeAnnotation) {
        return null;
    }

    @Override
    public SymbolInformation visitFunctionTypeAnnotation(FunctionTypeAnnotation typeAnnotation) {
        return null;
    }

    @Override
    public SymbolInformation visitGenericTypeAnnotation(GenericTypeAnnotation typeAnnotation) {
        return null;
    }

    @Override
    public SymbolInformation visitParameter(Parameter parameter) {
        return null;
    }

    public record AnalyzeResult(IRProgram irProgram, Set<String> globals) {

    }
}

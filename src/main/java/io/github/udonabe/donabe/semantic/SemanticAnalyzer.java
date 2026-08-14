package io.github.udonabe.donabe.semantic;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.runtime.VariableCell;
import io.github.udonabe.donabe.runtime.value.BuiltinFunctionValue;
import io.github.udonabe.donabe.runtime.value.UndefinedValue;
import io.github.udonabe.donabe.runtime.value.VoidValue;

import java.util.ArrayList;
import java.util.List;

public final class SemanticAnalyzer implements ASTVisitor<SymbolInformation> {
    static final BuiltinFunctionValue BUILTIN_PRINT = new BuiltinFunctionValue(
            List.of("target"),
            l -> {
                System.out.println(l.getFirst().display());
                return new VoidValue();
            }
    );
    private final Scope rootScope;
    private final String source;
    private Scope currentScope;
    private boolean inFunction;
    private List<VariableCell> resolution;

    public SemanticAnalyzer(String source) {
        this.source = source;
        this.rootScope = new Scope(null);
        this.rootScope.put("print", new SymbolInformation(false));
        this.currentScope = rootScope;
        this.inFunction = false;
        this.resolution = new ArrayList<>();

        resolution.add(new VariableCell(false, BUILTIN_PRINT));
        rootScope.putId("print", 0);
    }

    private int nextId() {
        return resolution.size();
    }

    public List<VariableCell> check(Program program) {
        program.accept(this);
        return List.copyOf(resolution);
    }

    private void defineFunctionsTemporary(Scope scope, List<Statement> statements) {
        statements.stream().filter(s -> s instanceof FunctionDefineStatement)
                .forEach(s -> {
                    FunctionDefineStatement functionDefineStatement = (FunctionDefineStatement) s;
                    scope.put(functionDefineStatement.identifier().name(), new SymbolInformation(false, true));
                });
    }

    private void pushScope() {
        currentScope = new Scope(currentScope);
    }

    private void popScope() {
        currentScope = currentScope.parent();
    }

    @Override
    public SymbolInformation visitProgram(Program program) {
        List<Statement> statements = program.statements();
        defineFunctionsTemporary(rootScope, statements);
        for (Statement statement : statements) {
            if (statement == null) continue;
            statement.accept(this);
        }
        return null;
    }

    @Override
    public SymbolInformation visitBlockStatement(BlockStatement statement) {
        pushScope();
        defineFunctionsTemporary(currentScope, statement.statements());
        for (Statement s : statement.statements()) {
            s.accept(this);
        }
        popScope();
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
        if (!currentScope.put(statement.identifier().name(), new SymbolInformation(false))) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source, "関数\"%s\"は既に定義されています。", statement.identifier().name()));
        }
        currentScope.putId(statement.identifier().name(), nextId());
        resolution.add(new VariableCell(false, new UndefinedValue()));
        List<String> argNames = statement.args().stream()
                .map(Identifier::name)
                .toList();

        Scope func = currentScope.capture();
        Scope before = currentScope;
        currentScope = func;
        for (String argName : argNames) {
            func.put(argName, new SymbolInformation(false));
            currentScope.putId(argName, nextId());
            resolution.add(new VariableCell(false, new UndefinedValue()));
        }
        inFunction = true;
        statement.block().accept(this);
        inFunction = false;
        currentScope = before;
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
        if (!currentScope.put(statement.name().name(), new SymbolInformation(false))) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source, "変数\"%s\"は既に宣言されています。", statement.name()));
        }
        int id = nextId();
        currentScope.putId(statement.name().name(), id);
        resolution.add(new VariableCell(false, new UndefinedValue()));
        statement.name().resolve(id);
        return null;
    }

    @Override
    public SymbolInformation visitReturnStatement(ReturnStatement statement) {
        if (!inFunction) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source, "return文は関数の外で使用できません。"));
        }
        statement.returnValue().accept(this);
        return null;
    }

    @Override
    public SymbolInformation visitVarDeclaration(VarDeclaration statement) {
        statement.expr().accept(this);
        if (!currentScope.put(statement.name().name(), new SymbolInformation(true))) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source, "変数\"%s\"は既に宣言されています。", statement.name()));
        }
        int id = nextId();
        currentScope.putId(statement.name().name(), id);
        resolution.add(new VariableCell(true, new UndefinedValue()));
        statement.name().resolve(id);
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
        pushScope();
        currentScope.put(statement.variable().name(), new SymbolInformation(false));
        currentScope.putId(statement.variable().name(), nextId());
        resolution.add(new VariableCell(false, new UndefinedValue()));
        statement.body().accept(this);
        popScope();
        return null;
    }

    @Override
    public SymbolInformation visitAssignExpression(AssignExpression expr) {
        var target = expr.target().accept(this);
        if (!target.isAssignable()) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source, "式\"%s\"へは代入できません。", expr.target().display()));
        }
        SymbolInformation result = expr.value().accept(this);
        if (expr.target() instanceof Identifier identifier) {
            String name = identifier.name();
            currentScope.changeSymbolInfo(name, result);
        }
        return result;
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
    public SymbolInformation visitDecrement(Decrement expr) {
        expr.target().accept(this);
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitFunctionLiteral(FunctionLiteral expr) {
        Scope capture = currentScope.capture();
        Scope before = currentScope;
        currentScope = capture;
        expr.block().accept(this);
        currentScope = before;
        return new SymbolInformation(false);
    }

    @Override
    public SymbolInformation visitIdentifier(Identifier expr) {
        SymbolInformation symbol = currentScope.get(expr.name());
        if (symbol == null) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source, "識別子\"%s\"は宣言されていません。", expr.name()));
        }
        expr.resolve(currentScope.getId(expr.name()));
        return symbol;
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
}

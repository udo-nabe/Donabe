package io.github.udonabe.donabe.semantic.resolve;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.ast.*;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ast.type.FunctionTypeAnnotation;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;
import io.github.udonabe.donabe.runtime.BuiltinFunctions;
import io.github.udonabe.donabe.runtime.VariableCell;
import io.github.udonabe.donabe.runtime.value.*;
import io.github.udonabe.donabe.semantic.Scope;
import io.github.udonabe.donabe.semantic.SymbolInformation;

import java.util.*;

public final class NameResolver implements ASTVisitor<Void> {
    private final Scope rootScope;
    private final Map<Integer, VariableCell> resolution;
    private final Map<ASTNode, Set<Integer>> localsASTNodeMap;
    private final String source;
    private Scope currentScope;

    public NameResolver(String source) {
        this.source = source;
        this.rootScope = Scope.generateRoot();
        this.currentScope = rootScope;
        this.resolution = new HashMap<>();

        putBuiltinFunction("print", BuiltinFunctions.BUILTIN_PRINT, 0);
        putBuiltinFunction("input", BuiltinFunctions.BUILTIN_INPUT, 1);
        putBuiltinFunction("string", BuiltinFunctions.BUILTIN_STRING, 2);
        putBuiltinFunction("length", BuiltinFunctions.BUILTIN_LENGTH, 3);
        putBuiltinFunction("int", BuiltinFunctions.BUILTIN_INT, 4);
        localsASTNodeMap = new IdentityHashMap<>();
    }


    private void putBuiltinFunction(String name, BuiltinFunctionValue value, int id) {
        resolution.put(id, new VariableCell(value));
        rootScope.put(name, new SymbolInformation(false));
        rootScope.putId(name, id);
    }

    public ResolveResult resolve(Program program) {
        rootScope.resetChildPos();
        program.accept(this);
        return new ResolveResult(rootScope, resolution, localsASTNodeMap);
    }

    private int nextId() {
        return resolution.size();
    }

    private void putIdentifier(Scope currentScope, String name, int id) {
        currentScope.putId(name, id);
        resolution.put(id, new VariableCell(new UndefinedValue()));
    }

    private void defineFunctions(List<Statement> statements) {
        var defines = statements.stream()
                .filter(s -> s instanceof FunctionDefineStatement)
                .map(s -> (FunctionDefineStatement) s)
                .toList();

        //相互再帰を可能にするため、先に全て仮登録する
        for (var define : defines) {
            currentScope.put(define.identifier().name(), new SymbolInformation(false, true));
            putIdentifier(currentScope, define.identifier().name(), nextId());
        }

        for (var define : defines) {
            var locals = defineFunction(define);
            localsASTNodeMap.put(define, locals);
        }
    }

    private Set<Integer> defineFunction(List<Parameter> params, BlockStatement block) {
        currentScope = currentScope.newChild();

        Set<Integer> locals = new HashSet<>();
        for (Parameter param : params) {
            String argName = param.name().name();

            int argID = nextId();
            currentScope.put(argName, new SymbolInformation(false));
            putIdentifier(currentScope, argName, argID);
            locals.add(argID);
        }

        List<Statement> statements = block.statements();
        defineFunctions(statements);

        for (Statement statement : statements) {
            statement.accept(this);
            switch (statement) {
                case LetDeclaration define -> locals.add(currentScope.getId(define.name().name()));
                case VarDeclaration define -> locals.add(currentScope.getId(define.name().name()));
                case FunctionDefineStatement define -> locals.add(currentScope.getId(define.identifier().name()));
                default -> {
                    //localsに追加する必要がないため、何もしない
                }
            }
        }

        currentScope = currentScope.parent();
        return locals;
    }

    private Set<Integer> defineFunction(FunctionDefineStatement define) {
        Identifier functionIdentifier = define.identifier();

        if (!currentScope.put(functionIdentifier.name(), new SymbolInformation(false))) {
            throw new CompileException(ErrorUtil.makeError(define.location(), source, "識別子\"%s\"は既に定義されています。", functionIdentifier.name()));
        }
        //仮登録されているため、getIdで取得できる
        int id = currentScope.getId(functionIdentifier.name());
        putIdentifier(currentScope, functionIdentifier.name(), id);
        return defineFunction(define.params(), define.block());
    }

    private void visitVariableDeclaration(Expression expr, Identifier identifier, boolean isAssignable, SourceFileLocation location) {
        if (!currentScope.put(identifier.name(), new SymbolInformation(isAssignable))) {
            throw new CompileException(ErrorUtil.makeError(location, source, "識別子\"%s\"は既に宣言されています。", identifier.name()));
        }
        putIdentifier(currentScope, identifier.name(), nextId());

        expr.accept(this);
    }

    @Override
    public Void visitProgram(Program program) {
        defineFunctions(program.statements());

        for (Statement statement : program.statements()) {
            if (statement == null) continue;
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visitBlockStatement(BlockStatement statement) {
        currentScope = currentScope.newChild();
        defineFunctions(statement.statements());

        for (Statement s : statement.statements()) {
            s.accept(this);
        }
        currentScope = currentScope.parent();
        return null;
    }

    @Override
    public Void visitEmptyStatement(EmptyStatement statement) {
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement statement) {
        statement.expression().accept(this);
        return null;
    }

    @Override
    public Void visitFunctionDefineStatement(FunctionDefineStatement statement) {
        //既に定義済みのため、何もしない
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatement statement) {
        statement.condition().accept(this);
        statement.thenBlock().accept(this);
        if (statement.elseBlock() != null) {
            statement.elseBlock().accept(this);
        }
        return null;
    }

    @Override
    public Void visitLetDeclaration(LetDeclaration statement) {
        visitVariableDeclaration(
                statement.expr(),
                statement.name(),
                false,
                statement.location()
        );
        return null;
    }

    @Override
    public Void visitVarDeclaration(VarDeclaration statement) {
        visitVariableDeclaration(
                statement.expr(),
                statement.name(),
                true,
                statement.location()
        );
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatement statement) {
        statement.condition().accept(this);
        statement.loop().accept(this);
        return null;
    }

    @Override
    public Void visitForEachStatement(ForEachStatement statement) {
        statement.iterable().accept(this);

        Scope inner = currentScope.newChild();
        Identifier variable = statement.variable();

        if (!currentScope.put(variable.name(), new SymbolInformation(false))) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source, "識別子\"%s\"は既に宣言されています。", variable.name()));
        }

        putIdentifier(currentScope, variable.name(), nextId());

        statement.body().accept(this);
        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatement statement) {
        statement.returnValue().accept(this);
        return null;
    }

    @Override
    public Void visitFunctionLiteral(FunctionLiteral literal) {
        var locals = defineFunction(literal.args(), literal.block());
        localsASTNodeMap.put(literal, locals);
        return null;
    }

    @Override
    public Void visitIdentifier(Identifier identifier) {
        if (currentScope.get(identifier.name()) == null) {
            throw new CompileException(ErrorUtil.makeError(identifier.location(), source, "識別子\"%s\"は宣言されていません。", identifier.name()));
        }
        return null;
    }

    @Override
    public Void visitBinaryExpression(BinaryExpression expr) {
        expr.left().accept(this);
        expr.right().accept(this);
        return null;
    }

    @Override
    public Void visitBooleanLiteral(BooleanLiteral expr) {
        return null;
    }

    @Override
    public Void visitCallExpression(CallExpression expr) {
        Expression callee = expr.target();
        callee.accept(this);

        for (Expression arg : expr.args()) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visitListLiteral(ListLiteral expression) {
        for (Expression e : expression.elements()) {
            e.accept(this);
        }
        return null;
    }

    @Override
    public Void visitStringLiteral(StringLiteral expr) {
        return null;
    }

    @Override
    public Void visitUnaryExpression(UnaryExpression expr) {
        expr.expr().accept(this);
        return null;
    }

    @Override
    public Void visitVoidExpression(VoidExpression expr) {
        return null;
    }

    @Override
    public Void visitNamedTypeAnnotation(NamedTypeAnnotation typeAnnotation) {
        return null;
    }

    @Override
    public Void visitFunctionTypeAnnotation(FunctionTypeAnnotation typeAnnotation) {
        return null;
    }

    @Override
    public Void visitParameter(Parameter parameter) {
        return null;
    }

    @Override
    public Void visitIndexExpression(IndexExpression expr) {
        expr.target().accept(this);
        expr.index().accept(this);
        return null;
    }

    @Override
    public Void visitIntegerLiteral(IntegerLiteral expr) {
        return null;
    }

    @Override
    public Void visitIncrement(Increment expr) {
        expr.target().accept(this);
        return null;
    }

    @Override
    public Void visitDecrement(Decrement expr) {
        expr.target().accept(this);
        return null;
    }

    @Override
    public Void visitAssignExpression(AssignExpression expr) {
        expr.target().accept(this);
        expr.value().accept(this);
        return null;
    }

    @Override
    public Void visitCompoundAssignExpression(CompoundAssignExpression expr) {
        expr.target().accept(this);
        expr.value().accept(this);
        return null;
    }

    public record ResolveResult(Scope root, Map<Integer, VariableCell> resolution, Map<ASTNode, Set<Integer>> localsASTNodeMap) {

    }
}

package io.github.udonabe.donabe.semantic.resolve;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ast.ASTNode;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.SourceFileLocation;
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
import io.github.udonabe.donabe.semantic.GlobalSymbol;
import io.github.udonabe.donabe.semantic.LocalSymbol;
import io.github.udonabe.donabe.semantic.Scope;
import io.github.udonabe.donabe.semantic.Symbol;
import io.github.udonabe.donabe.semantic.SymbolInformation;

public final class NameResolver implements ASTVisitor<Void> {

    private final Scope rootScope;
    private final Map<ASTNode, Integer> localCountASTNodeMap;
    private final Set<GlobalSymbol> globals;
    private final Map<Identifier, Symbol> resolutionMap;
    private final String source;
    private final ResolveContext context;
    private Scope currentScope;

    public NameResolver(String source) {
        this.source = source;
        this.rootScope = Scope.generateRoot();
        this.currentScope = rootScope;
        this.context = new ResolveContext();
        localCountASTNodeMap = new HashMap<>();
        resolutionMap = new HashMap<>();
        globals = new HashSet<>();

        putBuiltinFunction("print");
        putBuiltinFunction("input");
        putBuiltinFunction("range");
        putBuiltinFunction("now");
    }

    private void putBuiltinFunction(String name) {
        globals.add(new GlobalSymbol(name));
    }

    public ResolveResult resolve(Program program) {
        rootScope.resetChildPos();
        program.accept(this);
        return new ResolveResult(rootScope, localCountASTNodeMap, Map.copyOf(resolutionMap), globals);
    }

    private void putLocalIdentifier(Scope currentScope, Identifier identifier, int id) {
        currentScope.putId(identifier.name(), id);
        resolutionMap.put(identifier, new LocalSymbol(id));
    }

    private void putGlobalIdentifier(Identifier identifier) {
        globals.add(new GlobalSymbol(identifier.name()));
        resolutionMap.put(identifier, new GlobalSymbol(identifier.name()));
    }

    private void defineGlobals(List<Definition> statements) {
        //相互再帰を可能にするため、先に全て仮登録する
        for (var define : statements) {
            if (globals.contains(new GlobalSymbol(define.name().name()))) {
                throw new CompileException(ErrorUtil.makeError(define.location(), source, 
                        "Identifier '%s' is already declared.",
                        define.name().name()));
            }
            putGlobalIdentifier(define.name());
        }

        for (var define : statements) {
            if (define instanceof FunctionDefineStatement functionDefine) {
                var localCount = defineFunction(functionDefine.params(), functionDefine.block());
                localCountASTNodeMap.put(define, localCount);
            } else {
                define.accept(this);
            }
        }
    }

    private int defineFunction(List<Parameter> params, BlockStatement block) {
        currentScope = currentScope.newChild(true);
        context.pushFunction();

        for (Parameter param : params) {
            String argName = param.name().name();

            int argID = context.issueID();
            currentScope.put(argName, new SymbolInformation(false));
            putLocalIdentifier(currentScope, param.name(), argID);
        }

        for (Statement s : block.statements()) {
            s.accept(this);
        }

        int count = context.popFunction();
        currentScope = currentScope.parent();
        return count;
    }

    private void visitVariableDeclaration(Expression expr, Identifier identifier, boolean isAssignable,
            SourceFileLocation location) {
        if (context.isRoot()) {
            if (!globals.contains(new GlobalSymbol(identifier.name()))) {
                throw new CompileException(
                        ErrorUtil.makeError(location, source, "Local identifier \"%s\" has not yet declared.",
                                identifier.name()));
            }

            expr.accept(this);
        } else {
            if (!currentScope.put(identifier.name(), new SymbolInformation(isAssignable))) {
                throw new CompileException(
                        ErrorUtil.makeError(location, source, "Local identifier \"%s\" is already declared.",
                                identifier.name()));
            }

            expr.accept(this);
            putLocalIdentifier(currentScope, identifier, context.issueID());
        }
    }

    @Override
    public Void visitProgram(Program program) {
        defineGlobals(program.definitions());

        for (Statement statement : program.definitions()) {
            if (statement == null) {
                continue;
            }
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visitBlockStatement(BlockStatement statement) {
        currentScope = currentScope.newChild(false);

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
        if (!context.isRoot()) {
            localCountASTNodeMap.put(statement, defineFunction(statement.params(), statement.block()));
            int id = context.issueID();
            currentScope.put(statement.name().name(), new SymbolInformation(false));
            currentScope.putId(statement.name().name(), id);
            putLocalIdentifier(currentScope, statement.name(), id);
        }
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
                statement.location());
        return null;
    }

    @Override
    public Void visitVarDeclaration(VarDeclaration statement) {
        visitVariableDeclaration(
                statement.expr(),
                statement.name(),
                true,
                statement.location());
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

        currentScope.newChild(false);
        Identifier variable = statement.variable();

        if (!currentScope.put(variable.name(), new SymbolInformation(false))) {
            throw new CompileException(
                    ErrorUtil.makeError(statement.location(), source, "Local identifier \"%s\" is already declared.",
                            variable.name()));
        }

        putLocalIdentifier(currentScope, variable, context.issueID());

        statement.body().accept(this);

        currentScope = currentScope.parent();
        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatement statement) {
        statement.returnValue().accept(this);
        return null;
    }

    @Override
    public Void visitFunctionLiteral(FunctionLiteral literal) {
        int localCount = defineFunction(literal.args(), literal.block());
        localCountASTNodeMap.put(literal, localCount);
        return null;
    }

    @Override
    public Void visitIdentifier(Identifier identifier) {
        if (currentScope.get(identifier.name()) == null) {
            if (!globals.contains(new GlobalSymbol(identifier.name()))) {
                throw new CompileException(
                        ErrorUtil.makeError(identifier.location(), source,
                                "Identifier \"%s\" has not yet been declared.",
                                identifier.name()));
            }
            resolutionMap.put(identifier, new GlobalSymbol(identifier.name()));
            return null;
        }
        resolutionMap.put(identifier, currentScope.getId(identifier.name()));
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
    public Void visitMemberAccessExpression(MemberAccessExpression expr) {
        expr.target().accept(this);
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
    public Void visitGenericTypeAnnotation(GenericTypeAnnotation typeAnnotation) {
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

    public record ResolveResult(Scope root, Map<ASTNode, Integer> localCountASTNodeMap,
            Map<Identifier, Symbol> resolutionMap,
            Set<GlobalSymbol> globals) {

    }
}

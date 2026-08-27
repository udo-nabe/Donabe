package io.github.udonabe.donabe.semantic.type;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Parameter;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ast.type.FunctionTypeAnnotation;
import io.github.udonabe.donabe.ast.type.NamedTypeAnnotation;
import io.github.udonabe.donabe.ast.type.TypeAnnotation;
import io.github.udonabe.donabe.semantic.Scope;
import io.github.udonabe.donabe.semantic.flow.FlowAnalyzer;
import io.github.udonabe.donabe.semantic.flow.FlowInfo;
import io.github.udonabe.donabe.semantic.type.builtin.*;
import io.github.udonabe.donabe.semantic.type.function.FunctionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TypeChecker implements ASTVisitor<Type> {
    private final TypeResolver typeResolver;
    private final OperationChecker operationChecker;
    private final FlowAnalyzer flowAnalyzer;
    private final Map<Integer, Type> identifierTypeTable;
    private final TypeCheckerContext context;
    private final String source;
    private Scope currentScope;

    public TypeChecker(Scope rootScope, String source) {
        this.currentScope = rootScope;
        this.source = source;
        this.identifierTypeTable = new HashMap<>();
        typeResolver = new TypeResolver(source);
        context = new TypeCheckerContext();
        operationChecker = new OperationChecker(source);

        registerBuiltinFunctions();
        flowAnalyzer = new FlowAnalyzer();
    }

    public void check(Program program) {
        currentScope.resetChildPos();
        program.accept(this);
    }

    private Type defineFunction(List<Parameter> params, TypeAnnotation returnType, BlockStatement block, SourceFileLocation location) {
        context.pushReturnType(typeResolver.resolve(returnType));
        pushScope();

        for (Parameter param : params) {
            int paramID = currentScope.getId(param.name().name());
            Type paramType = typeResolver.resolve(param.type());
            identifierTypeTable.put(paramID, paramType);
        }

        defineFunctions(block.statements());

        for (Statement statement : block.statements()) {
            statement.accept(this);
        }

        FlowInfo functionFlow = flowAnalyzer.visitBlockStatement(block);    //フロー解析はステートレスのため、後から実行しても問題ない

        if (isMissingReturn(functionFlow.canFallThrough(), typeResolver.resolve(returnType))) {
            throw new CompileException(ErrorUtil.makeError(location, source,
                    "This function has a path that can exit without returning a value."));
        }

        popScope();
        context.popReturnType();

        return generateFunctionType(params, returnType);
    }

    private boolean isMissingReturn(boolean blockCanFallThrough, Type returnType) {
        return blockCanFallThrough && !(returnType instanceof VoidType);
    }

    private void defineFunction(FunctionDefineStatement statement) {
        defineFunction(statement.params(), statement.returnType(), statement.block(), statement.location());
    }

    private void defineFunctions(List<Statement> statements) {
        List<FunctionDefineStatement> defines = statements.stream()
                .filter(s -> s instanceof FunctionDefineStatement)
                .map(s -> (FunctionDefineStatement) s)
                .toList();

        //相互参照を可能にするため、先に名前と型だけ登録する
        for (FunctionDefineStatement define : defines) {
            Identifier functionName = define.identifier();
            List<Parameter> params = define.params();
            TypeAnnotation returnType = define.returnType();

            int functionID = currentScope.getId(functionName.name());
            FunctionType functionType = generateFunctionType(params, returnType);
            identifierTypeTable.put(functionID, functionType);
        }

        for (FunctionDefineStatement define : defines) {
            defineFunction(define);
        }
    }

    @Override
    public Type visitProgram(Program program) {
        defineFunctions(program.statements());
        for (Statement statement : program.statements()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Type visitBlockStatement(BlockStatement statement) {
        pushScope();
        defineFunctions(statement.statements());
        for (Statement s : statement.statements()) {
            s.accept(this);
        }
        popScope();
        return null;
    }

    @Override
    public Type visitEmptyStatement(EmptyStatement statement) {
        //すべきことが無い
        return null;
    }

    @Override
    public Type visitExpressionStatement(ExpressionStatement statement) {
        statement.expression().accept(this);
        return null;
    }

    @Override
    public Type visitFunctionDefineStatement(FunctionDefineStatement statement) {
        //既に定義済みのため、スキップ
        return null;
    }

    @Override
    public Type visitIfStatement(IfStatement statement) {
        Type conditionType = statement.condition().accept(this);
        if (!(conditionType instanceof BooleanType)) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source,
                    "The conditional expression of an 'if' statement must be of type 'Bool'. Actual: %s", conditionType));
        }
        statement.thenBlock().accept(this);
        if (statement.elseBlock() != null) {
            statement.elseBlock().accept(this);
        }
        return null;
    }

    private void declareVariable(Identifier identifier, TypeAnnotation typeAnnotation, Expression value, SourceFileLocation location) {
        Type identifierType = typeResolver.resolve(typeAnnotation);
        Type valueType = value.accept(this);

        if (!identifierType.isCompatible(valueType)) {
            throw new CompileException(ErrorUtil.makeError(location, source,
                    "Cannot initialize a value of type '%s' with a variable of type '%s'.",
                    identifierType.asString(), valueType.asString()));
        }

        int identifierID = currentScope.getId(identifier.name());

        identifierTypeTable.put(identifierID, identifierType);
    }

    @Override
    public Type visitLetDeclaration(LetDeclaration statement) {
        declareVariable(statement.name(), statement.type(), statement.expr(), statement.location());
        return null;
    }

    @Override
    public Type visitReturnStatement(ReturnStatement statement) {
        Type returnType = statement.returnValue().accept(this);
        Type expectReturnType = context.currentReturnType();

        if (!expectReturnType.equals(returnType)) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source,
                    "The return type is different from what was expected. Expected: %s, Actual: %s", expectReturnType.asString(), returnType.asString()));
        }

        return null;
    }

    @Override
    public Type visitVarDeclaration(VarDeclaration statement) {
        declareVariable(statement.name(), statement.type(), statement.expr(), statement.location());
        return null;
    }

    @Override
    public Type visitWhileStatement(WhileStatement statement) {
        Type conditionType = statement.condition().accept(this);
        if (!(conditionType instanceof BooleanType)) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source,
                    "The conditional expression of an 'while' statement must be of type 'Bool'. Actual: %s", conditionType));
        }

        statement.loop().accept(this);
        return null;
    }

    @Override
    public Type visitForEachStatement(ForEachStatement statement) {
        //まだ型パラメータを導入していないため、保留
        throw new UnsupportedOperationException("The for-each statement cannot be used currently.");
    }

    @Override
    public Type visitAssignExpression(AssignExpression expr) {
        Type targetType = expr.target().accept(this);
        Type valueType = expr.value().accept(this);

        if (!targetType.isCompatible(valueType)) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source,
                    "Cannot assign a value of type '%s' to a variable of type '%s'.",
                    targetType.asString(), valueType.asString()));
        }

        return targetType;
    }

    @Override
    public Type visitBinaryExpression(BinaryExpression expr) {
        Type lhs = expr.left().accept(this);
        Type rhs = expr.right().accept(this);

        return operationChecker.checkBinary(lhs, expr.operator(), rhs, expr.location());
    }

    @Override
    public Type visitBooleanLiteral(BooleanLiteral expr) {
        return new BooleanType();
    }

    @Override
    public Type visitCallExpression(CallExpression expr) {
        Type calleeType = expr.target().accept(this);

        if (!(calleeType instanceof FunctionType(List<Type> paramTypes, Type returnType))) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source,
                    "Cannot call anything other than a function."));
        }

        List<Type> argTypes = expr.args().stream()
                .map(e -> e.accept(this))
                .toList();

        if (paramTypes.size() != argTypes.size()) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source,
                    "The lengths of the formal parameter list and the actual argument list do not match. Expected: %d, Actual: %d",
                    paramTypes.size(), argTypes.size()));
        }

        boolean isArgsCompatible = IntStream.range(0, paramTypes.size())
                .allMatch(i -> paramTypes.get(i).isCompatible(argTypes.get(i)));

        if (!isArgsCompatible) {
            String paramsString = paramTypes.stream()
                    .map(Type::asString)
                    .collect(Collectors.joining(", ", "(", ")"));

            String argsString = argTypes.stream()
                    .map(Type::asString)
                    .collect(Collectors.joining(", ", "(", ")"));

            throw new CompileException(ErrorUtil.makeError(expr.location(), source,
                    "The types of the formal parameter list and the actual argument list do not match. Expected: %s, Actual: %s",
                    paramsString, argsString));
        }

        return returnType;
    }

    @Override
    public Type visitCompoundAssignExpression(CompoundAssignExpression expr) {
        Type targetType = expr.target().accept(this);
        Type valueType = expr.value().accept(this);

        operationChecker.checkCompoundAssign(targetType, expr.operator(), valueType, expr.location());

        return targetType;
    }

    @Override
    public Type visitDecrement(Decrement expr) {
        return operationChecker.checkIncrementAndDecrement(expr.target().accept(this), false, expr.location());
    }

    @Override
    public Type visitFunctionLiteral(FunctionLiteral expr) {
        return defineFunction(expr.args(), expr.type(), expr.block(), expr.location());
    }

    @Override
    public Type visitIdentifier(Identifier expr) {
        int identifierID = currentScope.getId(expr.name());
        if (!identifierTypeTable.containsKey(identifierID)) {
            throw new AssertionError("Identifier not found: " + expr.name() + ", slot: " + identifierID);
        }
        return identifierTypeTable.get(identifierID);
    }

    @Override
    public Type visitIncrement(Increment expr) {
        return operationChecker.checkIncrementAndDecrement(expr.target().accept(this), true, expr.location());
    }

    @Override
    public Type visitIndexExpression(IndexExpression expr) {
        //まだ型パラメータを導入していないため、保留
        throw new UnsupportedOperationException("The for-each statement cannot be used currently.");
    }

    @Override
    public Type visitIntegerLiteral(IntegerLiteral expr) {
        return new IntType();
    }

    @Override
    public Type visitListLiteral(ListLiteral expr) {
        //まだ型パラメータを導入していないため、保留
        throw new UnsupportedOperationException("The for-each statement cannot be used currently.");
    }

    @Override
    public Type visitStringLiteral(StringLiteral expr) {
        return new StringType();
    }

    @Override
    public Type visitUnaryExpression(UnaryExpression expr) {
        return operationChecker.checkUnary(expr.expr().accept(this), expr.operator(), expr.location());
    }

    @Override
    public Type visitVoidExpression(VoidExpression expr) {
        return new VoidType();
    }

    @Override
    public Type visitNamedTypeAnnotation(NamedTypeAnnotation typeAnnotation) {
        //ここに到達することは通常あり得ない
        throw new AssertionError("The function 'visitNamedTypeAnnotation' cannot be called.");
    }

    @Override
    public Type visitFunctionTypeAnnotation(FunctionTypeAnnotation typeAnnotation) {
        //ここに到達することは通常あり得ない
        throw new AssertionError("The function 'visitFunctionTypeAnnotation' cannot be called.");
    }

    @Override
    public Type visitParameter(Parameter parameter) {
        //ここに到達することは通常あり得ない
        throw new AssertionError("The function 'visitParameter' cannot be called.");
    }

    private void registerBuiltinFunctions() {
        identifierTypeTable.put(0,
                new FunctionType(
                        List.of(new AnyType()),
                        new VoidType()
                ));
        identifierTypeTable.put(1,
                new FunctionType(
                        List.of(new VoidType()),
                        new StringType()
                ));
        identifierTypeTable.put(2,
                new FunctionType(
                        List.of(new AnyType()),
                        new StringType()
                ));
        identifierTypeTable.put(3,
                new FunctionType(
                        List.of(new AnyType()),
                        new IntType()
                ));
        identifierTypeTable.put(4,
                new FunctionType(
                        List.of(new StringType()),
                        new IntType()
                ));
    }

    private void pushScope() {
        currentScope = currentScope.nextChildScope();
    }

    private void popScope() {
        currentScope = currentScope.parent();
    }

    private FunctionType generateFunctionType(List<Parameter> params, TypeAnnotation retType) {
        List<Type> types = params.stream()
                .map(Parameter::type)
                .map(typeResolver::resolve)
                .toList();
        return new FunctionType(types, typeResolver.resolve(retType));
    }

    Map<Integer, Type> identifierTypeTable() {
        return Map.copyOf(identifierTypeTable);
    }
}

package io.github.udonabe.donabe.semantic.type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.udonabe.donabe.CompileException;
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
import io.github.udonabe.donabe.ast.type.TypeAnnotation;
import io.github.udonabe.donabe.ast.type.UnknownTypeAnnotation;
import io.github.udonabe.donabe.error.ErrorUtil;
import io.github.udonabe.donabe.semantic.GlobalSymbol;
import io.github.udonabe.donabe.semantic.Symbol;
import io.github.udonabe.donabe.semantic.flow.FlowAnalyzer;
import io.github.udonabe.donabe.semantic.flow.FlowInfo;
import io.github.udonabe.donabe.semantic.type.builtin.AnyType;
import io.github.udonabe.donabe.semantic.type.builtin.BooleanType;
import io.github.udonabe.donabe.semantic.type.builtin.Int64Type;
import io.github.udonabe.donabe.semantic.type.builtin.IntType;
import io.github.udonabe.donabe.semantic.type.builtin.ListType;
import io.github.udonabe.donabe.semantic.type.builtin.StringType;
import io.github.udonabe.donabe.semantic.type.builtin.VoidType;
import io.github.udonabe.donabe.semantic.type.function.FunctionType;
import io.github.udonabe.donabe.semantic.type.inferrer.TypeInferrer;

public class TypeChecker implements ASTVisitor<Type> {

    private static final Logger log = LoggerFactory.getLogger(TypeChecker.class);
    private final TypeResolver typeResolver;
    private final TypeInferrer typeInferrer;
    private final OperationChecker operationChecker;
    private final FlowAnalyzer flowAnalyzer;
    private final Map<Symbol, Type> identifierTypeTable;
    private final TypeCheckerContext context;
    private final String source;
    private final Map<Identifier, Symbol> resolution;

    public TypeChecker(String source, Map<Identifier, Symbol> resolution) {
        this.source = source;
        this.resolution = resolution;
        this.identifierTypeTable = new HashMap<>();
        typeResolver = new TypeResolver(source);
        context = new TypeCheckerContext();
        operationChecker = new OperationChecker(source);

        registerBuiltinFunctions();
        flowAnalyzer = new FlowAnalyzer();
        typeInferrer = new TypeInferrer(source);
    }

    public void check(Program program) {
        program.accept(this);
    }

    private Type defineFunction(List<Parameter> params, TypeAnnotation returnType, BlockStatement block,
            SourceFileLocation location) {
        context.pushReturnType(typeResolver.resolve(returnType));

        for (Parameter param : params) {
            Symbol paramID = resolution.get(param.name());
            Type paramType = typeResolver.resolve(param.type());
            identifierTypeTable.put(paramID, paramType);
        }

        for (Statement statement : block.statements()) {
            statement.accept(this);
        }

        FlowInfo functionFlow = flowAnalyzer.visitBlockStatement(block); //フロー解析はステートレスのため、後から実行しても問題ない

        if (isMissingReturn(functionFlow.canFallThrough(), typeResolver.resolve(returnType))) {
            throw new CompileException(ErrorUtil.makeError(location, source,
                    "This function has a path that can exit without returning a value."));
        }

        context.popReturnType();

        return generateFunctionType(params, returnType);
    }

    private boolean isMissingReturn(boolean blockCanFallThrough, Type returnType) {
        return blockCanFallThrough && !(returnType instanceof VoidType);
    }

    private Type defineFunction(FunctionDefineStatement statement) {
        return defineFunction(statement.params(), statement.returnType(), statement.block(), statement.location());
    }

    private void defineGlobals(List<Definition> statements) {
        //相互参照を可能にするため、先に名前と型だけ登録する
        for (Definition define : statements) {
            if (define instanceof FunctionDefineStatement functionDefine) {
                Identifier functionName = functionDefine.name();
                List<Parameter> params = functionDefine.params();
                TypeAnnotation returnType = functionDefine.returnType();

                Symbol functionID = resolution.get(functionName);
                FunctionType functionType = generateFunctionType(params, returnType);
                identifierTypeTable.put(functionID, functionType);
            } else {
                Identifier functionName = define.name();
                TypeAnnotation definitionAnnotation = define.type();

                if (definitionAnnotation instanceof UnknownTypeAnnotation) {
                    throw new CompileException(ErrorUtil.makeError(define.location(), source,
                            "Type annotation is required for global variables."));
                }

                Symbol identifierID = resolution.get(functionName);
                identifierTypeTable.put(identifierID, typeResolver.resolve(definitionAnnotation));
            }
        }

        for (Definition definition : statements) {
            if (definition instanceof FunctionDefineStatement functionDefine) {
                defineFunction(functionDefine);
            } else {
                definition.accept(this);
            }
        }
    }

    @Override
    public Type visitProgram(Program program) {
        defineGlobals(program.definitions());
        for (Statement statement : program.definitions()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Type visitBlockStatement(BlockStatement statement) {
        for (Statement s : statement.statements()) {
            s.accept(this);
        }
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
        if (!context.isRoot()) {
            identifierTypeTable.put(
                    resolution.get(statement.name()),
                    defineFunction(statement)
            );
        }
        return null;
    }

    @Override
    public Type visitIfStatement(IfStatement statement) {
        Type conditionType = statement.condition().accept(this);
        if (!(conditionType instanceof BooleanType)) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source,
                    "The conditional expression of an 'if' statement must be of type 'Bool'. Actual: %s",
                    conditionType));
        }
        statement.thenBlock().accept(this);
        if (statement.elseBlock() != null) {
            statement.elseBlock().accept(this);
        }
        return null;
    }

    private void declareVariable(Identifier identifier, TypeAnnotation typeAnnotation, Expression value,
            SourceFileLocation location) {
        Type valueType = value.accept(this);
        Type identifierType = typeInferrer.inferVariableDeclaration(typeAnnotation, valueType);

        if (!identifierType.isSupertypeOf(valueType)) {
            throw new CompileException(ErrorUtil.makeError(location, source,
                    "Cannot initialize a value of type '%s' with a variable of type '%s'.",
                    identifierType.asString(), valueType.asString()));
        }

        if (!context.isRoot()) {
            Symbol identifierSymbol = resolution.get(identifier);

            identifierTypeTable.put(identifierSymbol, identifierType);
        }
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

        if (!expectReturnType.isSupertypeOf(returnType)) {
            throw new CompileException(ErrorUtil.makeError(statement.location(), source,
                    "The return type is different from what was expected. Expected: %s, Actual: %s",
                    expectReturnType.asString(), returnType.asString()));
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
                    "The conditional expression of an 'while' statement must be of type 'Bool'. Actual: %s",
                    conditionType));
        }

        statement.loop().accept(this);
        return null;
    }

    @Override
    public Type visitForEachStatement(ForEachStatement statement) {
        //まだfor-each文を実装していないため、保留
        throw new UnsupportedOperationException("The for-each statement cannot be used currently.");
    }

    @Override
    public Type visitAssignExpression(AssignExpression expr) {
        Type targetType = expr.target().accept(this);
        Type valueType = expr.value().accept(this);

        if (!targetType.isSupertypeOf(valueType)) {
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
                .allMatch(i -> paramTypes.get(i).isSupertypeOf(argTypes.get(i)));

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
        Symbol identifierSymbol = resolution.get(expr);
        if (!identifierTypeTable.containsKey(identifierSymbol)) {
            throw new AssertionError("Identifier not found: " + expr.name() + ", slot: " + identifierSymbol);
        }
        return identifierTypeTable.get(identifierSymbol);
    }

    @Override
    public Type visitIncrement(Increment expr) {
        return operationChecker.checkIncrementAndDecrement(expr.target().accept(this), true, expr.location());
    }

    @Override
    public Type visitIndexExpression(IndexExpression expr) {
        Type targetType = expr.target().accept(this);
        if (!(targetType instanceof ListType listType)) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source,
                    "Cannot access elements by index for types other than 'List'. Actual: %s", targetType.asString()));
        }

        Type indexType = expr.index().accept(this);
        if (!(indexType instanceof IntType)) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source,
                    "Only the 'Int' type can be used as an index. Actual: %s", indexType.asString()));
        }

        return listType.elementType();
    }

    @Override
    public Type visitIntegerLiteral(IntegerLiteral expr) {
        return new IntType();
    }

    @Override
    public Type visitListLiteral(ListLiteral expr) {
        Type elementsType = null;
        for (Expression listElement : expr.elements()) {
            Type elementType = listElement.accept(this);
            if (elementsType == null) {
                elementsType = elementType;
            } else if (!elementType.isSupertypeOf(elementsType)) {
                elementsType = new AnyType();
            }
            log.trace("ListLiteral: elementType = {}", elementType.asString());
            log.trace("ListLiteral: elementsType = {}", elementsType.asString());
        }
        elementsType = elementsType == null ? new AnyType() : elementsType;
        log.trace("ListLiteral: inference = {}", elementsType.asString());
        return new ListType(elementsType);
    }

    @Override
    public Type visitMemberAccessExpression(MemberAccessExpression expr) {
        Type targetType = expr.target().accept(this);
        String memberName = expr.member().name();

        MemberInfo member = targetType.findMember(memberName);
        if (member == null) {
            throw new CompileException(ErrorUtil.makeError(expr.location(), source,
                    "Type '%s' does not have a member '%s'.",
                    targetType.asString(), memberName));
        }

        return member.type();
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
    public Type visitGenericTypeAnnotation(GenericTypeAnnotation typeAnnotation) {
        //ここに到達することは通常あり得ない
        throw new AssertionError("The function 'visitGenericTypeAnnotation' cannot be called.");
    }

    @Override
    public Type visitParameter(Parameter parameter) {
        //ここに到達することは通常あり得ない
        throw new AssertionError("The function 'visitParameter' cannot be called.");
    }

    private void registerBuiltinFunctions() {
        identifierTypeTable.put(new GlobalSymbol("print"),
                new FunctionType(
                        List.of(new AnyType()),
                        new VoidType()));
        identifierTypeTable.put(new GlobalSymbol("input"),
                new FunctionType(
                        List.of(),
                        new StringType()));
        identifierTypeTable.put(new GlobalSymbol("range"),
                new FunctionType(
                        List.of(new IntType(), new IntType()),
                        new ListType(new IntType())));
        identifierTypeTable.put(new GlobalSymbol("now"),
                new FunctionType(
                        List.of(),
                        new Int64Type()));
    }

    private FunctionType generateFunctionType(List<Parameter> params, TypeAnnotation retType) {
        List<Type> types = params.stream()
                .map(Parameter::type)
                .map(typeResolver::resolve)
                .toList();
        return new FunctionType(types, typeResolver.resolve(retType));
    }

    Map<Symbol, Type> identifierTypeTable() {
        return Map.copyOf(identifierTypeTable);
    }
}

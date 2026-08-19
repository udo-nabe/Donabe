package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.runtime.context.InterpretContext;
import io.github.udonabe.donabe.runtime.context.stack.StackFrame;
import io.github.udonabe.donabe.runtime.value.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements ASTVisitor<RuntimeValue<?>> {
    private static final Logger log = LoggerFactory.getLogger(Interpreter.class);
    private final Program program;
    private final OperationRegistry registry;
    private final String source;
    private final InterpretContext context;

    public Interpreter(Program program, OperationRegistry registry, String source, Map<Integer, VariableCell> variables) {
        this.program = program;
        this.source = source;
        this.context = new InterpretContext(variables);
        this.registry = registry;
    }

    public void run() {
        program.accept(this);
    }

    private void defineFunctions(List<FunctionDefineStatement> statements) {
        for (var statement : statements) {
            List<Identifier> args = statement.args();
            FunctionValue function = new FunctionValue(
                    statement.identifier().name(),
                    args,
                    statement.block().statements(),
                    context.peekStackFrame(),
                    statement.locals());
            context.setVarValue(statement.identifier().id(), function);
        }
    }

    private void checkArgSize(List<?> formalArgs, List<?> actualArgs, SourceFileLocation functionLocation) {
        int formalSize = formalArgs.size();
        int actualSize = actualArgs.size();
        if (formalSize != actualSize) {
            throw new InterpreterException(ErrorUtil.makeError(functionLocation, source, "仮引数リスト(%d個)と実引数リスト(%d個)の長さが異なります。", formalSize, actualSize));
        }
    }

    private List<? extends RuntimeValue<?>> evalArgs(List<Expression> args) {
        return args.stream()
                .map(e -> e.accept(this))
                .toList();
    }

    private RuntimeValue<?> callFunction(FunctionValue functionValue, List<Expression> args, SourceFileLocation functionLocation) {
        var actualArgs = evalArgs(args);
        RuntimeValue<?> retValue = new VoidValue();

        checkArgSize(functionValue.formalArgs(), args, functionLocation);

        context.pushStackFrame(new StackFrame(
                functionValue.parent(),
                functionValue.name(),
                new HashMap<>(),
                functionValue.locals()
        ));

        List<Identifier> formalArgs = functionValue.formalArgs();
        for (int i = 0; i < formalArgs.size(); i++) {
            Identifier arg = formalArgs.get(i);
            int id = arg.id();
            RuntimeValue<?> value = actualArgs.get(i);
            context.setVarValue(id, value);
        }

        var functionDefines = functionValue.statements().stream()
                .filter(s -> s instanceof FunctionDefineStatement)
                .map(s -> (FunctionDefineStatement) s)
                .toList();
        defineFunctions(functionDefines);

        for (Statement statement : functionValue.statements()) {
            try {
                statement.accept(this);
            } catch (ReturnSignal ret) {
                retValue = ret.value();
                break;
            }
        }
        return retValue;
    }

    private RuntimeValue<?> callBuiltinFunction(BuiltinFunctionValue functionValue, List<Expression> args, SourceFileLocation functionLocation) {
        checkArgSize(functionValue.formalArgs(), args, functionLocation);
        var actualArgs = evalArgs(args);

        var callee = functionValue.content();
        RuntimeValue<?> retValue = callee.apply(actualArgs);
        return retValue;
    }

    private IntegerValue incrementOrDecrement(boolean increment, boolean prefix, Identifier target) {
        VariableCell cell = context.getVar(target.id());
        if (cell.value() instanceof IntegerValue(Integer before)) {
            IntegerValue after = new IntegerValue(increment ? before + 1 : before - 1);
            cell.setValue(after);
            return prefix ? after : new IntegerValue(before);
        }
        throw new InterpreterException(String.format("演算子\"++\"は型\"%s\"に適用できません。", cell.value().typeName()));
    }

    private RuntimeValue<?> evalBinary(Expression leftExpr, BinaryOperator operator, Expression rightExpr) {
        RuntimeValue<?> leftValue = leftExpr.accept(this);
        RuntimeValue<?> rightValue = rightExpr.accept(this);
        return registry.applyBinary(operator, leftValue, rightValue);
    }

    private RuntimeValue<?> evalUnary(UnaryOperator operator, Expression target) {
        RuntimeValue<?> expr = target.accept(this);
        return registry.applyUnary(operator, expr);
    }

    @Override
    public RuntimeValue<?> visitProgram(Program program) {
        defineFunctions(program.statements()
                .stream()
                .filter(s -> s instanceof FunctionDefineStatement)
                .map(s -> (FunctionDefineStatement) s)
                .toList());
        for (Statement statement : program.statements()) {
            try {
                statement.accept(this);
            } catch (ReturnSignal e) {
                throw new AssertionError("ReturnSignalが捕捉されませんでした。", e);
            }
        }
        return null;
    }

    @Override
    public RuntimeValue<?> visitBlockStatement(BlockStatement statement) {
        defineFunctions(statement.statements()
                .stream()
                .filter(s -> s instanceof FunctionDefineStatement)
                .map(s -> (FunctionDefineStatement) s)
                .toList());
        for (Statement s : statement.statements()) {
            s.accept(this);
        }
        return null;
    }

    @Override
    public RuntimeValue<?> visitEmptyStatement(EmptyStatement statement) {
        return null;
    }

    @Override
    public RuntimeValue<?> visitExpressionStatement(ExpressionStatement statement) {
        return statement.expression().accept(this);
    }

    @Override
    public RuntimeValue<?> visitFunctionDefineStatement(FunctionDefineStatement statement) {
        log.trace("Skipped FunctionDefineStatement: {}", statement);
        return null;
    }

    @Override
    public RuntimeValue<?> visitIfStatement(IfStatement statement) {
        RuntimeValue<?> condition = statement.condition().accept(this);
        if (condition instanceof BooleanValue(Boolean value)) {
            if (value) {
                statement.thenBlock().accept(this);
            } else if (statement.elseBlock() != null) {
                statement.elseBlock().accept(this);
            }
        } else {
            throw new InterpreterException(ErrorUtil.makeError(statement.location(), source, "if文の条件式はboolean型である必要があります。"));
        }
        return null;
    }

    @Override
    public RuntimeValue<?> visitLetDeclaration(LetDeclaration statement) {
        context.setVarValue(statement.name().id(), statement.expr().accept(this));
        return null;
    }

    @Override
    public RuntimeValue<?> visitReturnStatement(ReturnStatement statement) {
        throw new ReturnSignal(statement.returnValue().accept(this));
    }

    @Override
    public RuntimeValue<?> visitVarDeclaration(VarDeclaration statement) {
        context.setVarValue(statement.name().id(), statement.expr().accept(this));
        return null;
    }

    @Override
    public RuntimeValue<?> visitWhileStatement(WhileStatement statement) {
        RuntimeValue<?> condition = statement.condition().accept(this);
        if (condition instanceof BooleanValue(Boolean value)) {
            boolean isLoop = value;
            while (isLoop) {
                statement.loop().accept(this);
                isLoop = ((BooleanValue) statement.condition().accept(this)).value();
            }
        } else {
            throw new InterpreterException(ErrorUtil.makeError(statement.location(), source, "while文・for文の条件式は、boolean型である必要があります。"));
        }
        return null;
    }

    @Override
    public RuntimeValue<?> visitForEachStatement(ForEachStatement statement) {
        RuntimeValue<?> iterableRes = statement.iterable().accept(this);
        if (!(iterableRes instanceof ListValue(List<RuntimeValue<?>> iterable))) {
            throw new InterpreterException(ErrorUtil.makeError(statement.location(), source, "for-each文で、list型以外を使うことはできません。"));
        }
        for (RuntimeValue<?> it : iterable) {
            context.setVarValue(statement.variable().id(), it);
            statement.body().accept(this);
        }
        return null;
    }

    @Override
    public RuntimeValue<?> visitAssignExpression(AssignExpression expr) {
        Expression target = expr.target();
        RuntimeValue<?> value = expr.value().accept(this);

        return switch (target) {
            case Identifier identifier -> assignIdentifier(identifier, value);
            case IndexExpression indexExpression -> assignList(indexExpression, value);
            default ->
                    throw new InterpreterException(ErrorUtil.makeError(expr.location(), source, "式\"%s\"には代入できません。", expr.display()));
        };
    }

    private RuntimeValue<?> assignIdentifier(Identifier identifier, RuntimeValue<?> value) {
        context.setVarValue(identifier.id(), value);
        return value;
    }

    private RuntimeValue<?> assignList(IndexExpression index, RuntimeValue<?> value) {
        RuntimeValue<?> listRes = index.target().accept(this);
        if (!(listRes instanceof ListValue(List<RuntimeValue<?>> list))) {
            throw new InterpreterException(ErrorUtil.makeError(index.location(), source, "list型以外は添字アクセスすることができません。"));
        }
        RuntimeValue<?> indexRes = index.index().accept(this);
        if (!(indexRes instanceof IntegerValue(Integer i))) {
            throw new InterpreterException(ErrorUtil.makeError(index.location(), source, "式\"%s\"は添字として使用できません。int型である必要があります。", index.index().display()));
        }
        list.set(i, value);
        return list.get(i);
    }

    @Override
    public RuntimeValue<?> visitBinaryExpression(BinaryExpression expr) {
        return evalBinary(expr.left(), expr.operator(), expr.right());
    }

    @Override
    public RuntimeValue<?> visitBooleanLiteral(BooleanLiteral expr) {
        return new BooleanValue(expr.value());
    }

    @Override
    public RuntimeValue<?> visitCallExpression(CallExpression expr) {
        Expression callee = expr.target();
        List<Expression> args = expr.args();
        RuntimeValue<?> func = callee.accept(this);
        if (func instanceof FunctionValue functionValue) {
            return callFunction(functionValue, args, callee.location());
        } else if (func instanceof BuiltinFunctionValue functionValue) {
            return callBuiltinFunction(functionValue, args, callee.location());
        }
        log.debug("Invalid callee: {}", func);
        throw new InterpreterException(ErrorUtil.makeError(expr.location(), source, "呼び出せるのはfunction型のみです。"));
    }

    @Override
    public RuntimeValue<?> visitCompoundAssignExpression(CompoundAssignExpression expr) {
        Expression target = expr.target();
        RuntimeValue<?> value = expr.value().accept(this);

        BinaryOperator binOperator = switch (expr.operator()) {
            case PLUS -> BinaryOperator.PLUS;
            case MINUS -> BinaryOperator.MINUS;
            case DIVISION -> BinaryOperator.DIVISION;
            case MULTIPLICATION -> BinaryOperator.MULTIPLICATION;
        };

        RuntimeValue<?> calc = registry.applyBinary(binOperator,
                target.accept(this),
                value);

        return switch (target) {
            case Identifier identifier -> assignIdentifier(identifier, calc);
            case IndexExpression indexExpression -> assignList(indexExpression, calc);
            default ->
                    throw new InterpreterException(ErrorUtil.makeError(expr.location(), source, "式\"%s\"には代入できません。", expr.display()));
        };
    }

    @Override
    public RuntimeValue<?> visitDecrement(Decrement expr) {
        return incrementOrDecrement(false, expr.prefix(), expr.target());
    }

    @Override
    public RuntimeValue<?> visitFunctionLiteral(FunctionLiteral expr) {
        return new FunctionValue(
                null,
                expr.args(),
                expr.block().statements(),
                context.peekStackFrame(),
                expr.locals());
    }

    @Override
    public RuntimeValue<?> visitIdentifier(Identifier expr) {
        return context.getVar(expr.id()).value();
    }

    @Override
    public RuntimeValue<?> visitIncrement(Increment expr) {
        return incrementOrDecrement(true, expr.prefix(), expr.target());
    }

    @Override
    public RuntimeValue<?> visitIndexExpression(IndexExpression expr) {
        RuntimeValue<?> listRes = expr.target().accept(this);
        if (!(listRes instanceof ListValue(List<? extends RuntimeValue<?>> list))) {
            throw new InterpreterException(ErrorUtil.makeError(expr.location(), source, "list型以外は添字アクセスすることができません。"));
        }
        RuntimeValue<?> indexRes = expr.index().accept(this);
        if (!(indexRes instanceof IntegerValue(Integer i))) {
            throw new InterpreterException(ErrorUtil.makeError(expr.location(), source, "式\"%s\"は添字として使用できません。int型である必要があります。", expr.index()));
        }
        return list.get(i);
    }

    @Override
    public RuntimeValue<?> visitIntegerLiteral(IntegerLiteral expr) {
        return new IntegerValue(expr.value());
    }

    @Override
    public RuntimeValue<?> visitListLiteral(ListLiteral expr) {
        List<RuntimeValue<?>> result = new ArrayList<>();
        for (Expression e : expr.elements()) {
            result.add(e.accept(this));
        }
        return new ListValue(result);
    }

    @Override
    public RuntimeValue<?> visitStringLiteral(StringLiteral expr) {
        return new StringValue(expr.value());
    }

    @Override
    public RuntimeValue<?> visitUnaryExpression(UnaryExpression expr) {
        return evalUnary(expr.operator(), expr.expr());
    }

    @Override
    public RuntimeValue<?> visitVoidExpression(VoidExpression expr) {
        return new VoidValue();
    }
}

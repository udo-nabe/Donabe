package io.github.udonabe.donabe.runtime;

import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.runtime.value.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Interpreter implements ASTVisitor<RuntimeValue<?>> {
    private final Program program;
    private final Environment rootEnvironment;
    private final OperationRegistry registry;
    private final String source;
    private Environment currentEnvironment;
    private InterpreterFlags flags;

    public Interpreter(Program program, OperationRegistry registry, String source) {
        this.program = program;
        this.source = source;
        this.rootEnvironment = new Environment(null);
        this.currentEnvironment = rootEnvironment;
        this.registry = registry;
        this.flags = new InterpreterFlags();
    }

    public void run() {
        program.accept(this);
    }

    private void blockPreProcess(Environment environment, List<Statement> statements) {
        statements.stream().filter(s -> s instanceof FunctionDefineStatement)
                .forEach(s -> {
                    FunctionDefineStatement functionDefineStatement = (FunctionDefineStatement) s;
                    List<String> argNames = functionDefineStatement.args().stream()
                            .map(Identifier::name)
                            .toList();
                    environment.declare(functionDefineStatement.identifier().name(), new VariableCell(false, new FunctionValue(argNames, null, null), true));
                });
    }

    private RuntimeValue<?> callFunction(FunctionValue functionValue, List<Expression> args) {
        List<? extends RuntimeValue<?>> actualArgs = args.stream()
                .map(e -> e.accept(this))
                .toList();

        Environment captured = new Environment(null);
        var cap = functionValue.captures();
        for (String key : cap.keySet()) {
            captured.declare(key, cap.get(key));
        }

        List<String> argNames = functionValue.argNames();
        for (int i = 0; i < argNames.size(); i++) {
            String argName = argNames.get(i);
            captured.declareForce(argName, new VariableCell(false, actualArgs.get(i)));
        }

        RuntimeValue<?> retValue = new VoidValue();
        flags.inFunction = true;
        Environment before = currentEnvironment;
        currentEnvironment = captured;
        for (Statement statement : functionValue.statements()) {
            try {
                statement.accept(this);
            } catch (ReturnSignal ret) {
                retValue = ret.value();
                break;
            }
        }
        flags.inFunction = false;
        currentEnvironment = before;
        return retValue;
    }

    private IntegerValue incrementOrDecrement(boolean increment, boolean prefix, Identifier target) {
        RuntimeValue<?> value = currentEnvironment.getVar(target.name());
        if (value instanceof IntegerValue(Integer before)) {
            IntegerValue after = (IntegerValue) currentEnvironment.assign(target.name(), new IntegerValue(increment ? before + 1 : before - 1));
            return prefix ? after : new IntegerValue(before);
        }
        throw new InterpreterException(String.format("演算子\"++\"は型\"%s\"に適用できません。", value.typeName()));
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
        blockPreProcess(rootEnvironment, program.statements());
        for (Statement statement : program.statements()) {
            try {
                statement.accept(this);
            } catch (ReturnSignal e) {
                throw new AssertionError("ReturnSignalが捕捉されませんでした。", e);
            }
        }
        return null;
    }

    public void pushEnvironment() {
        currentEnvironment = new Environment(currentEnvironment);
    }

    public void popEnvironment() {
        currentEnvironment = currentEnvironment.parent();
    }

    @Override
    public RuntimeValue<?> visitBlockStatement(BlockStatement statement) {
        pushEnvironment();
        blockPreProcess(currentEnvironment, statement.statements());
        for (Statement s : statement.statements()) {
            s.accept(this);
        }
        popEnvironment();
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
        List<Identifier> args = statement.args();
        FunctionValue function = new FunctionValue(args.stream().map(Identifier::name).toList(), statement.block().statements(), currentEnvironment.getVarsRecursive());
        currentEnvironment.initialize(statement.identifier().name(), function);
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
        currentEnvironment.declare(statement.name(), new VariableCell(false, statement.expr().accept(this)));
        return null;
    }

    @Override
    public RuntimeValue<?> visitReturnStatement(ReturnStatement statement) {
        if (flags.inFunction) {
            throw new ReturnSignal(statement.returnValue().accept(this));
        } else {
            throw new InterpreterException(ErrorUtil.makeError(statement.location(), source, "return文は、関数の外で使用できません。"));
        }
    }

    @Override
    public RuntimeValue<?> visitVarDeclaration(VarDeclaration statement) {
        currentEnvironment.declare(statement.name(), new VariableCell(true, statement.expr().accept(this)));
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
            pushEnvironment();
            currentEnvironment.declare(statement.variable().name(), new VariableCell(false, it));
            statement.body().accept(this);
            popEnvironment();
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
            default -> throw new InterpreterException(ErrorUtil.makeError(expr.location(), source, "式\"%s\"には代入できません。", expr.display()));
        };
    }

    private RuntimeValue<?> assignIdentifier(Identifier identifier, RuntimeValue<?> value) {
        return currentEnvironment.assign(identifier.name(), value);
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
        if (callee instanceof Identifier identifier && identifier.name().equals("print")) {
            if (args.size() != 1)
                throw new InterpreterException(ErrorUtil.makeError(expr.location(), source, "関数\"%s\"は%dつの引数を要求しますが、実引数が%d個になっています。", "print", 1, args.size()));
            RuntimeValue<?> eval = args.getFirst().accept(this);
            System.out.println(eval.display());
            return new VoidValue();
        } else {
            RuntimeValue<?> func = callee.accept(this);
            if (!(func instanceof FunctionValue functionValue)) {
                throw new InterpreterException(ErrorUtil.makeError(expr.location(), source, "呼び出せるのはfunction型のみです。"));
            }
            return callFunction(functionValue, args);
        }
    }

    @Override
    public RuntimeValue<?> visitDecrement(Decrement expr) {
        return incrementOrDecrement(false, expr.prefix(), expr.target());
    }

    @Override
    public RuntimeValue<?> visitFunctionLiteral(FunctionLiteral expr) {
        List<String> argNames = expr.args().stream()
                .map(Identifier::name)
                .toList();
        return new FunctionValue(argNames, expr.block().statements(), currentEnvironment.getVarsRecursive());
    }

    @Override
    public RuntimeValue<?> visitIdentifier(Identifier expr) {
        return currentEnvironment.getVar(expr.name());
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

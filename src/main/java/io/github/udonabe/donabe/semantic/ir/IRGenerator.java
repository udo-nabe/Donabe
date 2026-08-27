package io.github.udonabe.donabe.semantic.ir;

import io.github.udonabe.donabe.ast.ASTNode;
import io.github.udonabe.donabe.ast.ASTVisitor;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.ir.IRProgram;
import io.github.udonabe.donabe.ir.instruction.*;
import io.github.udonabe.donabe.ir.instruction.label.Label;
import io.github.udonabe.donabe.runtime.value.*;
import io.github.udonabe.donabe.semantic.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IRGenerator implements ASTVisitor<List<Instruction>> {
    private final Scope rootScope;
    private final IRGenerateContext context;
    private final Map<ASTNode, Set<Integer>> localsASTNodeMap;
    private Scope currentScope;

    public IRGenerator(Scope rootScope, Set<Integer> resolution, Map<ASTNode, Set<Integer>> localsASTNodeMap) {
        this.rootScope = rootScope;
        this.currentScope = rootScope;
        this.localsASTNodeMap = localsASTNodeMap;
        context = new IRGenerateContext(resolution);
    }

    public IRProgram generate(Program program) {
        return new IRProgram(program.accept(this));
    }

    private List<Instruction> forEach(List<Statement> statements) {
        var result = new ArrayList<Instruction>();

        var defineFunctions = statements.stream()
                .filter(s -> s instanceof FunctionDefineStatement)
                .map(s -> (FunctionDefineStatement) s)
                .toList();
        for (FunctionDefineStatement define : defineFunctions) {
            result.addAll(defineFunction(define));
        }

        for (Statement statement : statements) {
            result.addAll(statement.accept(this));
        }

        return List.copyOf(result);
    }

    private List<Instruction> defineFunction(FunctionDefineStatement statement) {
        var result = new ArrayList<Instruction>();
        int functionID = currentScope.getId(statement.identifier().name());

        context.pushFunction(localsASTNodeMap.get(statement));
        currentScope = currentScope.nextChildScope();

        List<Integer> paramSlots = statement.args().stream()
                .map(identifier -> currentScope.getId(identifier.name()))
                .toList();

        for (Statement s : statement.block().statements()) {
            result.addAll(s.accept(this));
        }

        currentScope = currentScope.parent();
        context.popFunction();

        FunctionValue functionValue = new FunctionValue(
                statement.identifier().name(),
                paramSlots,
                context.currentLocals(),
                result
        );

        return List.of(
                new Push(functionValue),
                store(functionID)
        );
    }

    private Instruction load(int slot) {
        if (context.inFunction() && context.shouldUseLocal(slot)) {
            return new LoadLocal(slot);
        } else {
            return new LoadCaptured(slot);
        }
    }

    private Instruction store(int slot) {
        if (context.inFunction() && context.shouldUseLocal(slot)) {
            return new StoreLocal(slot);
        } else {
            return new StoreCaptured(slot);
        }
    }

    @Override
    public List<Instruction> visitProgram(Program program) {
        return forEach(program.statements());
    }

    @Override
    public List<Instruction> visitBlockStatement(BlockStatement statement) {
        currentScope = currentScope.nextChildScope();
        var result = forEach(statement.statements());
        currentScope = currentScope.parent();
        return result;
    }

    @Override
    public List<Instruction> visitEmptyStatement(EmptyStatement statement) {
        return List.of();
    }

    @Override
    public List<Instruction> visitExpressionStatement(ExpressionStatement statement) {
        var result = new ArrayList<>(statement.expression().accept(this));
        result.add(new Pop());
        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitFunctionDefineStatement(FunctionDefineStatement statement) {
        // 既に変換済みのため、何もしない
        return List.of();
    }

    @Override
    public List<Instruction> visitIfStatement(IfStatement statement) {
        var result = new ArrayList<Instruction>();

        if (statement.elseBlock() != null) {
            String elseLabel = context.nextLabel();
            String finLabel = context.nextLabel();

            result.addAll(statement.condition().accept(this));
            result.add(new JmpFalse(new Label(elseLabel)));

            result.addAll(statement.thenBlock().accept(this));
            result.add(new Jmp(new Label(finLabel)));

            result.add(new LabelNop(new Label(elseLabel)));
            result.addAll(statement.elseBlock().accept(this));
            result.add(new Jmp(new Label(finLabel)));

            result.add(new LabelNop(new Label(finLabel)));
        } else {
            String finLabel = context.nextLabel();

            result.addAll(statement.condition().accept(this));
            result.add(new JmpFalse(new Label(finLabel)));

            result.addAll(statement.thenBlock().accept(this));

            result.add(new LabelNop(new Label(finLabel)));
        }

        return List.copyOf(result);
    }

    private List<Instruction> assign(Expression target, Expression expr, boolean isDeclaration) {
        if (target instanceof Identifier identifier) {
            int identifierID = currentScope.getId(identifier.name());
            List<Instruction> exprInstructions = expr.accept(this);

            List<Instruction> result = new ArrayList<>();
            result.addAll(exprInstructions);
            result.add(store(identifierID));

            if (!isDeclaration) {
                result.add(load(identifierID));
            }

            return List.copyOf(result);
        } else {
            throw new UnsupportedOperationException("Assignment to anything other than an identifier is currently not supported.");
        }
    }

    @Override
    public List<Instruction> visitLetDeclaration(LetDeclaration statement) {
        return assign(statement.name(), statement.expr(), true);
    }

    @Override
    public List<Instruction> visitReturnStatement(ReturnStatement statement) {
        var result = new ArrayList<Instruction>();
        var returnValue = statement.returnValue();

        if (returnValue instanceof VoidExpression) {
            return List.of(new VoidReturn());
        } else {
            result.addAll(returnValue.accept(this));
            result.add(new Return());
            return List.copyOf(result);
        }
    }

    @Override
    public List<Instruction> visitVarDeclaration(VarDeclaration statement) {
        return assign(statement.name(), statement.expr(), true);
    }

    @Override
    public List<Instruction> visitWhileStatement(WhileStatement statement) {
        var result = new ArrayList<Instruction>();

        String loopLabel = context.nextLabel();
        String finLabel = context.nextLabel();

        result.add(new LabelNop(new Label(loopLabel)));

        result.addAll(statement.condition().accept(this));
        result.add(new JmpFalse(new Label(finLabel)));

        result.addAll(statement.loop().accept(this));

        result.add(new Jmp(new Label(loopLabel)));

        result.add(new LabelNop(new Label(finLabel)));

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitForEachStatement(ForEachStatement statement) {
        throw new UnsupportedOperationException("The statement for-each is not supported currently.");    //実装に追加の命令セットが多く必要になるため、未実装としておく
    }

    @Override
    public List<Instruction> visitAssignExpression(AssignExpression expr) {
        return assign(expr.target(), expr.value(), false);
    }

    @Override
    public List<Instruction> visitBinaryExpression(BinaryExpression expr) {
        List<Instruction> result = new ArrayList<>();

        result.addAll(expr.left().accept(this));
        result.addAll(expr.right().accept(this));
        result.add(switch (expr.operator()) {
            case PLUS -> new Add();
            case MINUS -> new Sub();
            case MULTIPLICATION -> new Mul();
            case DIVISION -> new Div();
            case EQUAL -> new Equal();
            case LESS -> new Less();
            case GREATER -> new Greater();
            case LESS_EQUAL -> new LessEqual();
            case GREATER_EQUAL -> new GreaterEqual();
        });

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitBooleanLiteral(BooleanLiteral expr) {
        var value = new BooleanValue(expr.value());
        return List.of(new Push(value));
    }

    @Override
    public List<Instruction> visitCallExpression(CallExpression expr) {
        var result = new ArrayList<Instruction>();

        List<Instruction> callee = expr.target().accept(this);

        for (Expression arg : expr.args().reversed()) {  //関数の呼び出し規約を守るため、リストを逆順にする
            result.addAll(arg.accept(this));
        }

        result.addAll(callee);
        result.add(new Call());

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitCompoundAssignExpression(CompoundAssignExpression expr) {
        if (expr.target() instanceof Identifier identifier) {
            int identifierID = currentScope.getId(identifier.name());
            List<Instruction> exprInstructions = expr.value().accept(this);

            List<Instruction> result = new ArrayList<>();

            result.add(load(identifierID));
            result.addAll(exprInstructions);
            result.add(switch (expr.operator()) {
                case PLUS -> new Add();
                case MINUS -> new Sub();
                case MULTIPLICATION -> new Mul();
                case DIVISION -> new Div();
            });

            result.add(store(identifierID));

            result.add(load(identifierID));

            return List.copyOf(result);
        } else {
            throw new UnsupportedOperationException("Compound assignment to anything other than an identifier is currently not supported.");
        }
    }

    private List<Instruction> incrementOrDecrement(Expression target, boolean prefix, boolean increment) {
        if (target instanceof Identifier identifier) {
            int identifierID = currentScope.getId(identifier.name());
            List<Instruction> result = new ArrayList<>();

            if (prefix) {
                result.add(load(identifierID));
                result.add(new Push(new IntegerValue(1)));
                result.add(increment ? new Add() : new Sub());

                result.add(store(identifierID));
                result.add(load(identifierID));
            } else {
                result.add(load(identifierID));
                result.add(load(identifierID));
                result.add(new Push(new IntegerValue(1)));
                result.add(increment ? new Add() : new Sub());

                result.add(store(identifierID));
            }

            return List.copyOf(result);
        } else {
            throw new UnsupportedOperationException("Decrement to anything other than an identifier is currently not supported.");
        }
    }

    @Override
    public List<Instruction> visitDecrement(Decrement expr) {
        return incrementOrDecrement(expr.target(), expr.prefix(), false);
    }

    @Override
    public List<Instruction> visitFunctionLiteral(FunctionLiteral expr) {
        context.pushFunction(localsASTNodeMap.get(expr));
        var result = new ArrayList<Instruction>();

        currentScope = currentScope.nextChildScope();

        List<Integer> paramSlots = expr.args().stream()
                .map(identifier -> currentScope.getId(identifier.name()))
                .toList();

        for (Statement s : expr.block().statements()) {
            result.addAll(s.accept(this));
        }
        currentScope = currentScope.parent();

        context.popFunction();

        FunctionValue functionValue = new FunctionValue(
                null,
                paramSlots,
                context.currentLocals(),
                result
        );
        return List.of(new Push(functionValue));
    }

    @Override
    public List<Instruction> visitIdentifier(Identifier expr) {
        int identifierID = currentScope.getId(expr.name());
        return List.of(load(identifierID));
    }

    @Override
    public List<Instruction> visitIncrement(Increment expr) {
        return incrementOrDecrement(expr.target(), expr.prefix(), true);
    }

    @Override
    public List<Instruction> visitIndexExpression(IndexExpression expr) {
        throw new UnsupportedOperationException("The statement for-each is not supported currently.");    //実装に追加の命令セットが多く必要になるため、未実装としておく
    }

    @Override
    public List<Instruction> visitIntegerLiteral(IntegerLiteral expr) {
        var value = new IntegerValue(expr.value());
        return List.of(new Push(value));
    }

    @Override
    public List<Instruction> visitListLiteral(ListLiteral expr) {
        throw new UnsupportedOperationException("The statement for-each is not supported currently.");    //実装に追加の命令セットが多く必要になるため、未実装としておく
    }

    @Override
    public List<Instruction> visitStringLiteral(StringLiteral expr) {
        var value = new StringValue(expr.value());
        return List.of(new Push(value));
    }

    @Override
    public List<Instruction> visitUnaryExpression(UnaryExpression expr) {
        var result = new ArrayList<Instruction>();

        result.addAll(expr.expr().accept(this));

        result.add(switch (expr.operator()) {
            case MINUS -> new Minus();
            case PLUS -> new Plus();
            case NOT -> new Not();
        });

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitVoidExpression(VoidExpression expr) {
        throw new AssertionError("VoidExpression cannot be visited.");  //VoidExpressionに到達することは通常ないため、AssertionErrorを出す。
    }
}

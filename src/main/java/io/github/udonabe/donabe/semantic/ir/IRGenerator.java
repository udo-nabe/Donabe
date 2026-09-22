package io.github.udonabe.donabe.semantic.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.github.udonabe.donabe.ir.IRLocation;
import io.github.udonabe.donabe.ir.IRProgram;
import io.github.udonabe.donabe.ir.instruction.Add;
import io.github.udonabe.donabe.ir.instruction.Call;
import io.github.udonabe.donabe.ir.instruction.Div;
import io.github.udonabe.donabe.ir.instruction.Equal;
import io.github.udonabe.donabe.ir.instruction.Greater;
import io.github.udonabe.donabe.ir.instruction.GreaterEqual;
import io.github.udonabe.donabe.ir.instruction.Index;
import io.github.udonabe.donabe.ir.instruction.Instruction;
import io.github.udonabe.donabe.ir.instruction.Jmp;
import io.github.udonabe.donabe.ir.instruction.JmpFalse;
import io.github.udonabe.donabe.ir.instruction.LabelNop;
import io.github.udonabe.donabe.ir.instruction.Less;
import io.github.udonabe.donabe.ir.instruction.LessEqual;
import io.github.udonabe.donabe.ir.instruction.LoadCaptured;
import io.github.udonabe.donabe.ir.instruction.LoadGlobal;
import io.github.udonabe.donabe.ir.instruction.LoadLocal;
import io.github.udonabe.donabe.ir.instruction.LoadMember;
import io.github.udonabe.donabe.ir.instruction.MakeList;
import io.github.udonabe.donabe.ir.instruction.Minus;
import io.github.udonabe.donabe.ir.instruction.Mul;
import io.github.udonabe.donabe.ir.instruction.Not;
import io.github.udonabe.donabe.ir.instruction.Plus;
import io.github.udonabe.donabe.ir.instruction.Pop;
import io.github.udonabe.donabe.ir.instruction.Push;
import io.github.udonabe.donabe.ir.instruction.Return;
import io.github.udonabe.donabe.ir.instruction.StoreCaptured;
import io.github.udonabe.donabe.ir.instruction.StoreGlobal;
import io.github.udonabe.donabe.ir.instruction.StoreLocal;
import io.github.udonabe.donabe.ir.instruction.Sub;
import io.github.udonabe.donabe.ir.instruction.VoidReturn;
import io.github.udonabe.donabe.ir.instruction.label.Label;
import io.github.udonabe.donabe.ir.value.BooleanValue;
import io.github.udonabe.donabe.ir.value.FunctionValue;
import io.github.udonabe.donabe.ir.value.IntegerValue;
import io.github.udonabe.donabe.ir.value.StringValue;
import io.github.udonabe.donabe.semantic.CaptureSymbol;
import io.github.udonabe.donabe.semantic.GlobalSymbol;
import io.github.udonabe.donabe.semantic.LocalSymbol;
import io.github.udonabe.donabe.semantic.Symbol;

public class IRGenerator implements ASTVisitor<List<Instruction>> {

    private static final Logger log = LoggerFactory.getLogger(IRGenerator.class);
    private final Map<Identifier, Symbol> resolution;
    private final IRGenerateContext context;
    private final Map<ASTNode, Integer> localCountASTNodeMap;

    public IRGenerator(Map<Identifier, Symbol> resolution, Set<GlobalSymbol> globals,
            Map<ASTNode, Integer> localCountASTNodeMap) {
        this.resolution = resolution;
        this.localCountASTNodeMap = localCountASTNodeMap;
        context = new IRGenerateContext(globals);
    }

    public IRProgram generate(Program program) {
        return new IRProgram(program.accept(this));
    }

    private List<Instruction> defineFunction(FunctionDefineStatement statement) {
        var result = new ArrayList<Instruction>();
        Symbol functionID = resolution.get(statement.name());

        context.pushFunction(localCountASTNodeMap.get(statement));

        List<Symbol> params = statement.params().stream()
                .map(parameter -> resolution.get(parameter.name()))
                .toList();
        if (!params.stream().allMatch(s -> s instanceof LocalSymbol)) {
            throw new IllegalStateException();
        }
        List<Integer> paramSlots = params.stream()
                .map(s -> (LocalSymbol) s)
                .map(s -> s.slot())
                .toList();

        for (Statement s : statement.block().statements()) {
            result.addAll(s.accept(this));
        }

        if (result.isEmpty()
                || (!(result.getLast() instanceof Return)
                && !(result.getLast() instanceof VoidReturn))) {
            result.add(new VoidReturn(generateLocation(statement.location())));
        }

        Set<Symbol> locals = context.currentLocals();
        if (!locals.stream().allMatch(s -> s instanceof LocalSymbol)) {
            throw new IllegalStateException();
        }
        int localCount = locals.size();

        FunctionValue functionValue = new FunctionValue(
                statement.name().name(),
                paramSlots,
                localCount,
                result);

        context.popFunction();

        return List.of(
                new Push(functionValue, generateLocation(statement.location())),
                store(functionID, statement.location()));
    }

    private Instruction load(Symbol symbol, SourceFileLocation location) {
        return switch (symbol) {
            case LocalSymbol local ->
                new LoadLocal(local.slot(), generateLocation(location));
            case CaptureSymbol capture ->
                new LoadCaptured(capture.slot(), generateLocation(location));
            case GlobalSymbol global ->
                new LoadGlobal(global.fullyQualifiedName(), generateLocation(location));
        };
    }

    private Instruction store(Symbol symbol, SourceFileLocation location) {
        return switch (symbol) {
            case LocalSymbol local ->
                new StoreLocal(local.slot(), generateLocation(location));
            case CaptureSymbol capture ->
                new StoreCaptured(capture.slot(), generateLocation(location));
            case GlobalSymbol global ->
                new StoreGlobal(global.fullyQualifiedName(), generateLocation(location));
        };
    }

    @Override
    public List<Instruction> visitProgram(Program program) {
        var result = new ArrayList<Instruction>();
        
        for (Statement statement : program.definitions()) {
            result.addAll(statement.accept(this));
        }

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitBlockStatement(BlockStatement statement) {
        var result = new ArrayList<Instruction>();

        for (Statement s : statement.statements()) {
            result.addAll(s.accept(this));
        }

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitEmptyStatement(EmptyStatement statement) {
        return List.of();
    }

    @Override
    public List<Instruction> visitExpressionStatement(ExpressionStatement statement) {
        var result = new ArrayList<>(statement.expression().accept(this));
        result.add(new Pop(generateLocation(statement.location())));
        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitFunctionDefineStatement(FunctionDefineStatement statement) {
        return defineFunction(statement);
    }

    @Override
    public List<Instruction> visitIfStatement(IfStatement statement) {
        var result = new ArrayList<Instruction>();

        if (statement.elseBlock() != null) {
            String elseLabel = context.nextLabel();
            String finLabel = context.nextLabel();

            //条件がfalseならelseへ飛ぶ
            result.addAll(statement.condition().accept(this));
            result.add(new JmpFalse(new Label(elseLabel), generateLocation(statement.location())));

            //thenブロック
            result.addAll(statement.thenBlock().accept(this));
            result.add(new Jmp(new Label(finLabel), generateLocation(statement.thenBlock().location())));

            //elseブロック
            result.add(new LabelNop(new Label(elseLabel), generateLocation(statement.elseBlock().location())));
            result.addAll(statement.elseBlock().accept(this));
            result.add(new Jmp(new Label(finLabel), generateLocation(statement.elseBlock().location())));

            //最終ジャンプ先
            result.add(new LabelNop(new Label(finLabel), generateLocation(statement.location())));
        } else {
            String finLabel = context.nextLabel();

            //条件がfalseならthenを飛ばす
            result.addAll(statement.condition().accept(this));
            result.add(new JmpFalse(new Label(finLabel), generateLocation(statement.thenBlock().location())));

            //thenブロック
            result.addAll(statement.thenBlock().accept(this));

            //最終ジャンプ先
            result.add(new LabelNop(new Label(finLabel), generateLocation(statement.location())));
        }

        return List.copyOf(result);
    }

    private List<Instruction> assign(Expression target, Expression expr, boolean isDeclaration) {
        if (target instanceof Identifier identifier) {
            Symbol identifierID = resolution.get(identifier);
            List<Instruction> exprInstructions = expr.accept(this);

            List<Instruction> result = new ArrayList<>();
            result.addAll(exprInstructions);
            result.add(store(identifierID, identifier.location()));

            if (!isDeclaration) {
                result.add(load(identifierID, identifier.location()));
            }

            return List.copyOf(result);
        } else {
            throw new UnsupportedOperationException(
                    "Assignment to anything other than an identifier is currently not supported.");
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
            return List.of(new VoidReturn(generateLocation(statement.location())));
        } else {
            result.addAll(returnValue.accept(this));
            result.add(new Return(generateLocation(statement.location())));
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

        result.add(new LabelNop(new Label(loopLabel), generateLocation(statement.location())));

        result.addAll(statement.condition().accept(this));
        result.add(new JmpFalse(new Label(finLabel), generateLocation(statement.location())));

        result.addAll(statement.loop().accept(this));

        result.add(new Jmp(new Label(loopLabel), generateLocation(statement.location())));

        result.add(new LabelNop(new Label(finLabel), generateLocation(statement.location())));

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitForEachStatement(ForEachStatement statement) {
        throw new UnsupportedOperationException("The statement for-each is not supported currently."); //実装に追加の命令セットが多く必要になるため、未実装としておく
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
            case PLUS ->
                new Add(generateLocation(expr.location()));
            case MINUS ->
                new Sub(generateLocation(expr.location()));
            case MULTIPLICATION ->
                new Mul(generateLocation(expr.location()));
            case DIVISION ->
                new Div(generateLocation(expr.location()));
            case EQUAL ->
                new Equal(generateLocation(expr.location()));
            case LESS ->
                new Less(generateLocation(expr.location()));
            case GREATER ->
                new Greater(generateLocation(expr.location()));
            case LESS_EQUAL ->
                new LessEqual(generateLocation(expr.location()));
            case GREATER_EQUAL ->
                new GreaterEqual(generateLocation(expr.location()));
        });

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitBooleanLiteral(BooleanLiteral expr) {
        var value = new BooleanValue(expr.value());
        return List.of(new Push(value, generateLocation(expr.location())));
    }

    @Override
    public List<Instruction> visitCallExpression(CallExpression expr) {
        var result = new ArrayList<Instruction>();

        List<Instruction> callee = expr.target().accept(this);

        for (Expression arg : expr.args().reversed()) { //関数の呼び出し規約を守るため、リストを逆順にする
            result.addAll(arg.accept(this));
        }

        result.addAll(callee);
        result.add(new Call(generateLocation(expr.location())));

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitCompoundAssignExpression(CompoundAssignExpression expr) {
        if (expr.target() instanceof Identifier identifier) {
            Symbol identifierID = resolution.get(identifier);
            List<Instruction> exprInstructions = expr.value().accept(this);

            List<Instruction> result = new ArrayList<>();

            result.add(load(identifierID, expr.location()));
            result.addAll(exprInstructions);
            result.add(switch (expr.operator()) {
                case PLUS ->
                    new Add(generateLocation(expr.location()));
                case MINUS ->
                    new Sub(generateLocation(expr.location()));
                case MULTIPLICATION ->
                    new Mul(generateLocation(expr.location()));
                case DIVISION ->
                    new Div(generateLocation(expr.location()));
            });

            result.add(store(identifierID, expr.location()));

            result.add(load(identifierID, expr.location()));

            return List.copyOf(result);
        } else {
            throw new UnsupportedOperationException(
                    "Compound assignment to anything other than an identifier is currently not supported.");
        }
    }

    private List<Instruction> incrementOrDecrement(Expression target, boolean prefix, boolean increment) {
        if (target instanceof Identifier identifier) {
            Symbol identifierID = resolution.get(identifier);
            List<Instruction> result = new ArrayList<>();

            if (prefix) {
                result.add(load(identifierID, identifier.location()));
                result.add(new Push(new IntegerValue(1), generateLocation(identifier.location())));
                result.add(increment ? new Add(generateLocation(identifier.location()))
                        : new Sub(generateLocation(identifier.location())));

                result.add(store(identifierID, identifier.location()));
                result.add(load(identifierID, identifier.location()));
            } else {
                result.add(load(identifierID, identifier.location()));
                result.add(load(identifierID, identifier.location()));
                result.add(new Push(new IntegerValue(1), generateLocation(identifier.location())));
                result.add(increment ? new Add(generateLocation(identifier.location()))
                        : new Sub(generateLocation(identifier.location())));

                result.add(store(identifierID, identifier.location()));
            }

            return List.copyOf(result);
        } else {
            throw new UnsupportedOperationException(
                    "Decrement to anything other than an identifier is currently not supported.");
        }
    }

    @Override
    public List<Instruction> visitDecrement(Decrement expr) {
        return incrementOrDecrement(expr.target(), expr.prefix(), false);
    }

    @Override
    public List<Instruction> visitFunctionLiteral(FunctionLiteral expr) {
        context.pushFunction(localCountASTNodeMap.get(expr));
        var result = new ArrayList<Instruction>();

        List<Integer> paramSlots = expr.args().stream()
                .map(parameter -> resolution.get(parameter.name()))
                .map(s -> ((LocalSymbol) s).slot())
                .toList();

        for (Statement s : expr.block().statements()) {
            result.addAll(s.accept(this));
        }

        if (result.isEmpty()
                || (!(result.getLast() instanceof Return)
                && !(result.getLast() instanceof VoidReturn))) {
            result.add(new VoidReturn(generateLocation(expr.location())));
        }

        FunctionValue functionValue = new FunctionValue(
                null,
                paramSlots,
                context.currentLocals().size(),
                result);

        context.popFunction();
        return List.of(new Push(functionValue, generateLocation(expr.location())));
    }

    @Override
    public List<Instruction> visitIdentifier(Identifier expr) {
        Symbol identifierID = resolution.get(expr);
        return List.of(load(identifierID, expr.location()));
    }

    @Override
    public List<Instruction> visitIncrement(Increment expr) {
        return incrementOrDecrement(expr.target(), expr.prefix(), true);
    }

    @Override
    public List<Instruction> visitIndexExpression(IndexExpression expr) {
        var result = new ArrayList<Instruction>();
        result.addAll(expr.target().accept(this));
        result.addAll(expr.index().accept(this));
        result.add(new Index(generateLocation(expr.location())));
        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitIntegerLiteral(IntegerLiteral expr) {
        var value = new IntegerValue(expr.value());
        return List.of(new Push(value, generateLocation(expr.location())));
    }

    @Override
    public List<Instruction> visitListLiteral(ListLiteral expr) {
        var result = new ArrayList<Instruction>();

        for (Expression arg : expr.elements()) {
            result.addAll(arg.accept(this));
        }

        result.add(new MakeList(expr.elements().size(), generateLocation(expr.location())));

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitMemberAccessExpression(MemberAccessExpression expr) {
        var result = new ArrayList<Instruction>();

        result.addAll(expr.target().accept(this));
        result.add(new LoadMember(expr.member().name(), generateLocation(expr.location())));

        log.trace("load_member: name={}", expr.member().name());
        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitStringLiteral(StringLiteral expr) {
        var value = new StringValue(expr.value());
        return List.of(new Push(value, generateLocation(expr.location())));
    }

    @Override
    public List<Instruction> visitUnaryExpression(UnaryExpression expr) {
        var result = new ArrayList<Instruction>();

        result.addAll(expr.expr().accept(this));

        result.add(switch (expr.operator()) {
            case MINUS ->
                new Minus(generateLocation(expr.location()));
            case PLUS ->
                new Plus(generateLocation(expr.location()));
            case NOT ->
                new Not(generateLocation(expr.location()));
        });

        return List.copyOf(result);
    }

    @Override
    public List<Instruction> visitVoidExpression(VoidExpression expr) {
        throw new AssertionError("VoidExpression cannot be visited."); //VoidExpressionに到達することは通常ないため、AssertionErrorを出す。
    }

    @Override
    public List<Instruction> visitNamedTypeAnnotation(NamedTypeAnnotation typeAnnotation) {
        throw new AssertionError("NamedTypeAnnotation cannot be visited."); //NamedTypeAnnotationに到達することは通常ないため、AssertionErrorを出す。
    }

    @Override
    public List<Instruction> visitFunctionTypeAnnotation(FunctionTypeAnnotation typeAnnotation) {
        throw new AssertionError("FunctionTypeAnnotation cannot be visited."); //FunctionTypeAnnotationに到達することは通常ないため、AssertionErrorを出す。
    }

    @Override
    public List<Instruction> visitGenericTypeAnnotation(GenericTypeAnnotation typeAnnotation) {
        throw new AssertionError("GenericTypeAnnotation cannot be visited."); //GenericTypeAnnotationに到達することは通常ないため、AssertionErrorを出す。

    }

    @Override
    public List<Instruction> visitParameter(Parameter parameter) {
        throw new AssertionError("Parameter cannot be visited."); //Parameterに到達することは通常ないため、AssertionErrorを出す。
    }

    private IRLocation generateLocation(SourceFileLocation location) {
        return new IRLocation(location.line());
    }
}

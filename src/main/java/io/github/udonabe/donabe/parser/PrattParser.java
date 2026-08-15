package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.ErrorUtil;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.*;
import io.github.udonabe.donabe.lexer.Token;

import java.util.List;
import java.util.Map;

import static io.github.udonabe.donabe.lexer.Token.Kind.*;
import static io.github.udonabe.donabe.parser.BasicParsers.*;
import static io.github.udonabe.donabe.parser.Parsers.separatedBy;
import static io.github.udonabe.donabe.parser.Parsers.token;
import static java.util.Map.entry;

class PrattParser {
    public static final Parser<FunctionLiteral> functionDefine =
            token(FUNC)
                    .then(separatedBy(identifier, token(COMMA)).between(token(LPAREN), token(RPAREN)))
                    .then(blockStatement())
                    .map(p -> new FunctionLiteral(p.getLeft().getRight(), p.getRight(), genLocation(p.getLeft().getLeft())));
    private static final int PREFIX_PRECEDENCE = 100;
    private static final int MIN_PRECEDENCE = 1;
    private static final Map<Token.Kind, Integer> PRECEDENCES = Map.ofEntries(
            entry(Token.Kind.PLUS, 60),
            entry(Token.Kind.MINUS, 60),
            entry(Token.Kind.ASTERISK, 70),
            entry(Token.Kind.SLASH, 70),
            entry(Token.Kind.EQUAL, 40),
            entry(Token.Kind.LESS, 50),
            entry(Token.Kind.GREATER, 50),
            entry(Token.Kind.LESS_EQUAL, 50),
            entry(Token.Kind.GREATER_EQUAL, 50),
            entry(Token.Kind.ASSIGN, MIN_PRECEDENCE),
            entry(Token.Kind.PLUS_ASSIGN, MIN_PRECEDENCE),
            entry(Token.Kind.MINUS_ASSIGN, MIN_PRECEDENCE),
            entry(Token.Kind.ASTERISK_ASSIGN, MIN_PRECEDENCE),
            entry(Token.Kind.SLASH_ASSIGN, MIN_PRECEDENCE)
    );
    private TokenStream stream;

    PrattParser(TokenStream stream) {
        this.stream = stream;
    }

    private static SourceFileLocation genLocation(Token token) {
        return new SourceFileLocation(token.line(), token.column());
    }

    Expression parseExpression(int precedence) {
        if (stream.peek().kind() == Token.Kind.RBRACE) {
            throw new CompileException(ErrorUtil.makeCompileError(stream.peek(), stream.peek().lexeme(), "expression"));
        }
        Expression left = parsePrefix();

        while (true) {
            Token.Kind next = stream.peek().kind();

            if (next == Token.Kind.LPAREN) {
                left = parseCall(left);
                continue;
            } else if (next == LBRACKET) {
                left = parseIndex(left);
                continue;
            } else if (next == Token.Kind.INCREMENT) {
                left = parseIncrement(left);
                continue;
            } else if (next == Token.Kind.DECREMENT) {
                left = parseDecrement(left);
                continue;
            }

            if (!PRECEDENCES.containsKey(next)) {
                return left;
            }
            int operatorPrecedence = PRECEDENCES.get(next);
            if (operatorPrecedence <= precedence) break;
            left = parseInfix(left, operatorPrecedence);
        }
        return left;
    }

    private Expression parsePrefix() {
        TokenStream before = stream.fork();
        Token prefix = stream.advance();
        return switch (prefix.kind()) {
            case INTEGER -> new IntegerLiteral(Integer.parseInt(prefix.lexeme()), genLocation(prefix));
            case STRING -> new StringLiteral(prefix.lexeme(), genLocation(prefix));
            case TRUE, FALSE -> new BooleanLiteral(Boolean.parseBoolean(prefix.lexeme()), genLocation(prefix));
            case IDENTIFIER -> new Identifier(prefix.lexeme(), genLocation(prefix));
            case FUNC -> {
                var result = functionDefine.parse(before);
                if (result instanceof ParseSuccess<FunctionLiteral>(FunctionLiteral value)) {
                    stream.from(before);
                    yield value;
                }
                throw new CompileException(((ParseFailed<FunctionLiteral>) result).message());
            }
            case LPAREN -> {
                Expression expr = parseExpression(0);
                stream.consume(Token.Kind.RPAREN);
                yield expr;
            }
            case LBRACKET -> {
                ParseResult<List<Expression>> elementsRes = Parsers.separatedBy(
                        BasicParsers.expression,
                        Parsers.token(Token.Kind.COMMA)
                ).parse(stream);
                if (!(elementsRes instanceof ParseSuccess<List<Expression>>(List<Expression> elements))) {
                    System.out.println(elementsRes);
                    throw new CompileException(((ParseFailed<?>) elementsRes).message());
                }
                stream.consume(RBRACKET);
                yield new ListLiteral(elements, genLocation(prefix));
            }
            case PLUS ->
                    new UnaryExpression(UnaryOperator.PLUS, parseExpression(PREFIX_PRECEDENCE), genLocation(prefix));
            case MINUS ->
                    new UnaryExpression(UnaryOperator.MINUS, parseExpression(PREFIX_PRECEDENCE), genLocation(prefix));
            case EXCLAMATION ->
                    new UnaryExpression(UnaryOperator.NOT, parseExpression(PREFIX_PRECEDENCE), genLocation(prefix));
            case INCREMENT -> {
                Identifier identifier = new Identifier(stream.consume(Token.Kind.IDENTIFIER).lexeme(), genLocation(prefix));
                yield new Increment(identifier, true, genLocation(prefix));
            }
            case DECREMENT -> {
                Identifier identifier = new Identifier(stream.consume(Token.Kind.IDENTIFIER).lexeme(), genLocation(prefix));
                yield new Decrement(identifier, true, genLocation(prefix));
            }
            default -> throw new CompileException(ErrorUtil.makeCompileError(prefix, prefix.lexeme(), "expression"));
        };
    }

    private Expression parseInfix(Expression prefix, int precedence) {
        Token infix = stream.advance();
        return switch (infix.kind()) {
            case PLUS ->
                    new BinaryExpression(prefix, BinaryOperator.PLUS, parseExpression(precedence), prefix.location());
            case MINUS ->
                    new BinaryExpression(prefix, BinaryOperator.MINUS, parseExpression(precedence), prefix.location());
            case ASTERISK ->
                    new BinaryExpression(prefix, BinaryOperator.MULTIPLICATION, parseExpression(precedence), prefix.location());
            case SLASH ->
                    new BinaryExpression(prefix, BinaryOperator.DIVISION, parseExpression(precedence), prefix.location());
            case EQUAL ->
                    new BinaryExpression(prefix, BinaryOperator.EQUAL, parseExpression(precedence), prefix.location());
            case LESS ->
                    new BinaryExpression(prefix, BinaryOperator.LESS, parseExpression(precedence), prefix.location());
            case GREATER ->
                    new BinaryExpression(prefix, BinaryOperator.GREATER, parseExpression(precedence), prefix.location());
            case LESS_EQUAL ->
                    new BinaryExpression(prefix, BinaryOperator.LESS_EQUAL, parseExpression(precedence), prefix.location());
            case GREATER_EQUAL ->
                    new BinaryExpression(prefix, BinaryOperator.GREATER_EQUAL, parseExpression(precedence), prefix.location());
            case ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, ASTERISK_ASSIGN, SLASH_ASSIGN -> assign(prefix, infix, precedence);
            default -> throw new CompileException(ErrorUtil.makeCompileError(infix, infix.lexeme(), "operator"));
        };
    }

    private Expression assign(Expression prefix, Token infix, int precedence) {
        return switch (infix.kind()) {
            case ASSIGN -> new AssignExpression(prefix, parseExpression(precedence - 1), prefix.location());
            case PLUS_ASSIGN ->
                    new CompoundAssignExpression(prefix, CompoundAssignOperator.PLUS, parseExpression(precedence - 1), prefix.location());
            case MINUS_ASSIGN ->
                    new CompoundAssignExpression(prefix, CompoundAssignOperator.MINUS, parseExpression(precedence - 1), prefix.location());
            case ASTERISK_ASSIGN ->
                    new CompoundAssignExpression(prefix, CompoundAssignOperator.MULTIPLICATION, parseExpression(precedence - 1), prefix.location());
            case SLASH_ASSIGN ->
                    new CompoundAssignExpression(prefix, CompoundAssignOperator.DIVISION, parseExpression(precedence - 1), prefix.location());
            default -> throw new IllegalStateException("PrattParser#assign has a problem.");
        };
    }

    private Expression parseCall(Expression prefix) {
        stream.consume(Token.Kind.LPAREN);
        ParseResult<List<Expression>> argsRes = Parsers.separatedBy(
                BasicParsers.expression,
                Parsers.token(Token.Kind.COMMA)
        ).parse(stream);
        if (!(argsRes instanceof ParseSuccess<List<Expression>>(List<Expression> args))) {
            System.out.println(argsRes);
            throw new CompileException(((ParseFailed<?>) argsRes).message());
        }
        stream.consume(Token.Kind.RPAREN);
        return new CallExpression(prefix, args, prefix.location());
    }

    private Expression parseIndex(Expression prefix) {
        stream.consume(LBRACKET);
        ParseResult<Expression> indexRes = expression.parse(stream);
        if (!(indexRes instanceof ParseSuccess<Expression>(Expression index))) {
            System.out.println(indexRes);
            throw new CompileException(((ParseFailed<?>) indexRes).message());
        }
        stream.consume(RBRACKET);
        return new IndexExpression(prefix, index, prefix.location());
    }

    private Increment parseIncrement(Expression prefix) {
        if (prefix instanceof Identifier identifier) {
            stream.consume(Token.Kind.INCREMENT);
            return new Increment(identifier, false, prefix.location());
        } else {
            throw new CompileException("[line %d, column %d] Unexpected expression: ".formatted(prefix.location().line(), prefix.location().column()) + prefix + ". Expected: IDENTIFIER.");
        }
    }

    private Decrement parseDecrement(Expression prefix) {
        if (prefix instanceof Identifier identifier) {
            stream.consume(Token.Kind.DECREMENT);
            return new Decrement(identifier, false, prefix.location());
        } else {
            throw new CompileException("[line %d, column %d] Unexpected expression: ".formatted(prefix.location().line(), prefix.location().column()) + prefix + ". Expected: IDENTIFIER.");
        }
    }
}

package io.github.udonabe.donabe.parser;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.SourceFileLocation;
import io.github.udonabe.donabe.ast.expr.Expression;
import io.github.udonabe.donabe.ast.expr.FunctionLiteral;
import io.github.udonabe.donabe.ast.expr.Identifier;
import io.github.udonabe.donabe.ast.expr.VoidExpression;
import io.github.udonabe.donabe.ast.statement.*;
import io.github.udonabe.donabe.lexer.Token;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.udonabe.donabe.lexer.Token.Kind.*;
import static io.github.udonabe.donabe.parser.Parsers.*;

public final class BasicParsers {
    public static final Parser<Identifier> identifier = token(IDENTIFIER).map(t -> new Identifier(t.lexeme(), genLocation(t)));

    public static final Parser<Expression> expression =
            stream -> {
                TokenStream fork = stream.fork();
                try {
                    var res = new PrattParser(fork).parseExpression(0);
                    stream.from(fork);
                    return new ParseSuccess<>(res);
                } catch (CompileException e) {
                    return new ParseFailed<>(e.getMessage(), fork.pos());
                }
            };

    public static final Parser<LetDeclaration> letDeclaration =
            token(LET)
                    .then(identifier)
                    .skip(token(ASSIGN))
                    .then(expression)
                    .skip(token(SEMICOLON))
                    .map(p -> new LetDeclaration(p.getLeft().getRight().name(), p.getRight(), genLocation(p.getKey().getLeft())));
    public static final Parser<VarDeclaration> varDeclaration =
            token(VAR)
                    .then(identifier)
                    .skip(token(ASSIGN))
                    .then(expression)
                    .skip(token(SEMICOLON))
                    .map(p -> new VarDeclaration(p.getLeft().getRight().name(), p.getRight(), genLocation(p.getKey().getLeft())));

    public static final Parser<ExpressionStatement> expressionStatement =
            expression.map(e -> new ExpressionStatement(e, e.location()))
                    .skip(token(SEMICOLON));
    public static Parser<WhileStatement> whileStatement =
            token(WHILE).then(expression)
                    .then(blockStatement())
                    .map(p -> new WhileStatement(p.getLeft().getRight(), p.getRight(), genLocation(p.getLeft().getLeft())));
    @SuppressWarnings("unchecked")
    public static Parser<BlockStatement> forStatement =
            sequence(
                    token(FOR).then(varDeclaration),
                    expression.skip(token(SEMICOLON)),
                    expression.skip(token(SEMICOLON).optional()),
                    blockStatement()
            ).map(l -> {
                var tokenVarDeclarationPair = (Pair<Token, VarDeclaration>) l.get(0);
                Expression condition = (Expression) l.get(1);
                Expression update = (Expression) l.get(2);
                BlockStatement block = (BlockStatement) l.get(3);

                SourceFileLocation location = genLocation(tokenVarDeclarationPair.getLeft());

                // for文をwhile文へ変換する(ループブロック内=処理→更新)
                List<Statement> statementsInLoop = block.statements();
                statementsInLoop.addLast(new ExpressionStatement(update, location));

                // スコープを正しくするため、全体をブロックで囲む
                List<Statement> outer = new ArrayList<>();
                outer.add(tokenVarDeclarationPair.getRight());
                outer.add(new WhileStatement(condition, new BlockStatement(statementsInLoop, location), location));
                return new BlockStatement(outer, location);
            });
    public static Parser<ReturnStatement> returnStatement =
            token(RETURN).then(expression.optional()).skip(token(SEMICOLON))
                    .map(p -> {
                        var location = genLocation(p.getLeft());
                        Optional<Expression> op = p.getRight();
                        return op.map(value -> new ReturnStatement(value, location)).orElseGet(() -> new ReturnStatement(new VoidExpression(location), location));
                    });
    public static Parser<FunctionDefineStatement> functionDefineStatement =
            token(FUNC).then(identifier)
                    .then(separatedBy(identifier, token(COMMA)).between(token(LPAREN), token(RPAREN)))
                    .then(blockStatement())
                    .map(p -> {
                        Identifier identifier = p.getLeft().getLeft().getRight();
                        List<Identifier> params = p.getLeft().getRight();
                        BlockStatement block = p.getRight();
                        var location = genLocation(p.getLeft().getLeft().getLeft());

                        return new FunctionDefineStatement(identifier, params, block, location);
                    });
    public static Parser<EmptyStatement> emptyStatement = token(SEMICOLON).map(t -> new EmptyStatement(genLocation(t)));
    public static Parser<ForEachStatement> forEachStatement = token(FOR)
            .skip(token(LET)).then(identifier)
            .skip(token(IN)).then(expression)
            .then(blockStatement())
            .map(p -> new ForEachStatement(p.getLeft().getLeft().getRight(), p.getLeft().getRight(), p.getRight(), genLocation(p.getLeft().getLeft().getLeft())));
    public static final Parser<Statement> statement = or(
            emptyStatement,
            letDeclaration,
            varDeclaration,
            expressionStatement,
            blockStatement(),
            ifStatement(),
            whileStatement,
            forStatement,
            returnStatement,
            functionDefineStatement,
            forEachStatement);
    public static final Parser<Program> program = removeIf(many(statement), t -> t instanceof EmptyStatement)
            .skip(token(EOF)).map(l -> new Program(l, new SourceFileLocation(0, 0)));

    @SuppressWarnings("unchecked")
    public static Parser<IfStatement> ifStatement() {
        return sequence(
                token(IF).then(expression),
                blockStatement(),
                token(ELSE).to(or(blockStatement(), lazy(BasicParsers::ifStatement))).optional()
        ).map(l -> {
            Expression condition = ((Pair<Token, Expression>) l.get(0)).getRight();
            BlockStatement ifBlock = (BlockStatement) l.get(1);
            Optional<Statement> elseBlockOptional = (Optional<Statement>) l.get(2);
            var location = genLocation(((Pair<Token, Expression>) l.get(0)).getLeft());

            return elseBlockOptional.map(blockStatement -> new IfStatement(condition, ifBlock, blockStatement, location))
                    .orElseGet(() -> new IfStatement(condition, ifBlock, null, location));
        });
    }

    public static Parser<BlockStatement> blockStatement() {
        return token(LBRACE)
                .then(removeIf(many(lazy(() -> statement)), t -> t instanceof EmptyStatement))
                .skip(token(RBRACE))
                .map(p -> new BlockStatement(p.getRight(), genLocation(p.getLeft())));
    }

    private static SourceFileLocation genLocation(Token token) {
        return new SourceFileLocation(token.line(), token.column());
    }
}

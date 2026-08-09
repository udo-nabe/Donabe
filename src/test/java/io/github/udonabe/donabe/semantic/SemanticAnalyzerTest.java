package io.github.udonabe.donabe.semantic;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.lexer.Lexer;
import io.github.udonabe.donabe.parser.BasicParsers;
import io.github.udonabe.donabe.parser.ParseFailed;
import io.github.udonabe.donabe.parser.ParseResult;
import io.github.udonabe.donabe.parser.ParseSuccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SemanticAnalyzerTest {
    private Program parse(String source) {
        TokenStream s = new Lexer(source).toTokenStream();
        ParseResult<Program> res = BasicParsers.program.parse(s);
        if (res instanceof ParseSuccess<Program>(Program value)) {
            return value;
        }
        throw new IllegalArgumentException("Could not parse source: " + ((ParseFailed<Program>) res).message());
    }
    private void doesNotThrow(String source) {
        var program = parse(source);
        assertDoesNotThrow(() -> new SemanticAnalyzer(source).check(program));
    }
    private void throwCompileException(String source) {
        var program = parse(source);
        assertThrows(CompileException.class, () -> new SemanticAnalyzer(source).check(program));
    }
    @Test
    void declaredLetCanBeReferenced() {
        doesNotThrow("""
                let foo = 1;
                foo;
                """);
    }
    @Test
    void declaredVarCanBeReferenced() {
        doesNotThrow("""
                var foo = 1;
                foo;
                """);
    }
    @Test
    void varCanBeAssigned() {
        doesNotThrow("""
                var foo = 1;
                foo = 2;
                """);
    }
    @Test
    void letCannotBeAssigned() {
        throwCompileException("""
                let foo = 1;
                foo = 2;
                """);
    }
    @Test
    void undefinedIdentifierCannotBeReferenced() {
        throwCompileException("""
                foo;
                """);
        throwCompileException("""
                foo;
                let foo = 1;
                """);
    }
    @Test
    void undefinedIdentifierCannotBeCalled() {
        throwCompileException("""
                foo();
                """);
        throwCompileException("""
                foo;
                let foo = func() {};
                """);
    }
    @Test
    void varOfParentScopeCanBeReferenced() {
        doesNotThrow("""
                let x = 1;
                {x;}
                """);
    }
    @Test
    void varOfChildScopeCannotBeReferenced() {
        throwCompileException("""
                {let x = 1;}
                x;
                """);
    }
    @Test
    void childScopeVarCanBeReferenced() {
        doesNotThrow("""
                {
                    let x = 1;
                    x;
                }
                """);
    }
    @Test
    void canShadowing() {
        doesNotThrow("""
                let x = 1;
                {
                    let x = 2;
                    x;
                }
                x;
                """);
    }
    @Test
    void varCanBeReferencedInExpression() {
        doesNotThrow("""
                let x = 1;
                print(x + 1 / 2);
                """);
    }
    @Test
    void undefinedVarCannotBeReferencedInExpression() {
        throwCompileException("""
                let x = 1;
                print(y + 1 / 2);
                """);
    }
}
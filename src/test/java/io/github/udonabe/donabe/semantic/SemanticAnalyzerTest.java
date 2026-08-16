package io.github.udonabe.donabe.semantic;

import io.github.udonabe.donabe.CompileException;
import io.github.udonabe.donabe.TokenStream;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.lexer.Lexer;
import io.github.udonabe.donabe.parser.BasicParsers;
import io.github.udonabe.donabe.parser.ParseFailed;
import io.github.udonabe.donabe.parser.ParseResult;
import io.github.udonabe.donabe.parser.ParseSuccess;
import io.github.udonabe.donabe.runtime.VariableCell;
import io.github.udonabe.donabe.runtime.value.UndefinedValue;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private void nameResolution(String source, List<VariableCell> resolution) {
        Lexer l = new Lexer(source);
        ParseResult<Program> programResult = BasicParsers.program.parse(l.toTokenStream());

        if (!(programResult instanceof ParseSuccess<Program>(Program value))) {
            fail();
            return; //到達不可能。コンパイルを通すため。
        }

        List<VariableCell> nameResolutions = new SemanticAnalyzer(source).check(value);
        assertEquals(resolution, nameResolutions);
    }

    @Test
    void nameResolutionBasic() {
        //正常系
        nameResolution("""
                        let a = 10;
                        var b = 2;
                        func add(a, b) { return a + b;};
                        let c = add(a, b);
                        print("ADD: " + c);
                        """,
                List.of(
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_PRINT),    //print
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_INPUT),    //input
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_STRING),    //string
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_LENGTH),    //length
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_RANGE),    //range
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_INT),    //int

                        new VariableCell(false, new UndefinedValue()),  //func add
                        new VariableCell(false, new UndefinedValue()),  //-> a
                        new VariableCell(false, new UndefinedValue()),  //-> b

                        new VariableCell(false, new UndefinedValue()),  //let a
                        new VariableCell(true, new UndefinedValue()),   //var b

                        new VariableCell(false, new UndefinedValue())   //let c
                ));
    }

    @Test
    void nameResolutionForEach() {
        nameResolution("""
                        let list = ["Hello", "Udon", "Nabe", "Donabe"];
                        for let i in list {
                            print(i);
                        }
                        """,
                List.of(
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_PRINT),    //print
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_INPUT),    //input
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_STRING),    //string
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_LENGTH),    //length
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_RANGE),    //range
                        new VariableCell(false, SemanticAnalyzer.BUILTIN_INT),    //int

                        new VariableCell(false, new UndefinedValue()),  //let list
                        new VariableCell(false, new UndefinedValue())   //let i
                ));
    }
}
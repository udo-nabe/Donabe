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
import io.github.udonabe.donabe.semantic.resolve.NameResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
                let foo: int = 1;
                foo;
                """);
    }

    @Test
    void declaredVarCanBeReferenced() {
        doesNotThrow("""
                var foo: int = 1;
                foo;
                """);
    }

    @Test
    void varCanBeAssigned() {
        doesNotThrow("""
                var foo: int = 1;
                foo = 2;
                """);
    }

    @Test
    void letCannotBeAssigned() {
        throwCompileException("""
                let foo: int = 1;
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
                let foo: int = 1;
                """);
    }

    @Test
    void undefinedIdentifierCannotBeCalled() {
        throwCompileException("""
                foo();
                """);
        throwCompileException("""
                foo;
                let foo: () -> void = func(): void {};
                """);
    }

    @Test
    void varOfParentScopeCanBeReferenced() {
        doesNotThrow("""
                let x: int = 1;
                {x;}
                """);
    }

    @Test
    void varOfChildScopeCannotBeReferenced() {
        throwCompileException("""
                {let x: int = 1;}
                x;
                """);
    }

    @Test
    void childScopeVarCanBeReferenced() {
        doesNotThrow("""
                {
                    let x: int = 1;
                    x;
                }
                """);
    }

    @Test
    void canShadowing() {
        doesNotThrow("""
                let x: int = 1;
                {
                    let x: int = 2;
                    x;
                }
                x;
                """);
    }

    @Test
    void varCanBeReferencedInExpression() {
        doesNotThrow("""
                let x: int = 1;
                print(x + 1 / 2);
                """);
    }

    @Test
    void undefinedVarCannotBeReferencedInExpression() {
        throwCompileException("""
                let x: int = 1;
                print(y + 1 / 2);
                """);
    }

    @Test
    void mutualRecursion() {
        doesNotThrow("""
                func a() -> void {
                    b();
                }
                func b() -> void {
                    a();
                }
                """);
    }

    @Test
    void doubleDeclaration() {
        throwCompileException("""
                func a() -> void {
                    
                }
                func a() -> void {
                    
                }
                """);
    }

    private void nameResolution(String source, Map<Integer, VariableCell> resolution) {
        Lexer l = new Lexer(source);
        ParseResult<Program> programResult = BasicParsers.program.parse(l.toTokenStream());

        if (!(programResult instanceof ParseSuccess<Program>(Program value))) {
            fail();
            return; //到達不可能。コンパイルを通すため。
        }

        Map<Integer, VariableCell> nameResolutions = new SemanticAnalyzer(source).check(value).resolution();
        assertEquals(resolution, nameResolutions);
    }

    @Test
    void nameResolutionBasic() {
        //正常系
        nameResolution("""
                        let a: int = 10;
                        var b: int = 2;
                        func add(a: int, b: int) -> int { return a + b;};
                        let c: int = add(a, b);
                        print("ADD: " + c);
                        """,
                Map.ofEntries(
                        Map.entry(0, new VariableCell(NameResolver.BUILTIN_PRINT)),    //print
                        Map.entry(1, new VariableCell(NameResolver.BUILTIN_INPUT)),    //input
                        Map.entry(2, new VariableCell(NameResolver.BUILTIN_STRING)),    //string
                        Map.entry(3, new VariableCell(NameResolver.BUILTIN_LENGTH)),    //length
                        Map.entry(4, new VariableCell(NameResolver.BUILTIN_RANGE)),    //range
                        Map.entry(5, new VariableCell(NameResolver.BUILTIN_INT)),    //int

                        Map.entry(6, new VariableCell(new UndefinedValue())),  //func add
                        Map.entry(7, new VariableCell(new UndefinedValue())),  //add->a
                        Map.entry(8, new VariableCell(new UndefinedValue())),  //add->b

                        Map.entry(9, new VariableCell(new UndefinedValue())),  //let a
                        Map.entry(10, new VariableCell(new UndefinedValue())),  //var b
                        Map.entry(11, new VariableCell(new UndefinedValue()))  //let c
                ));
    }

    @Test
    void nameResolutionForEach() {
        //現在サポートされていないため、一旦テストをしない。
//        nameResolution("""
//                        let list = ["Hello", "Udon", "Nabe", "Donabe"];
//                        for let i in list {
//                            print(i);
//                        }
//                        """,
//                Map.ofEntries(
//                        Map.entry(0, new VariableCell(NameResolver.BUILTIN_PRINT)),    //print
//                        Map.entry(1, new VariableCell(NameResolver.BUILTIN_INPUT)),    //input
//                        Map.entry(2, new VariableCell(NameResolver.BUILTIN_STRING)),    //string
//                        Map.entry(3, new VariableCell(NameResolver.BUILTIN_LENGTH)),    //length
//                        Map.entry(4, new VariableCell(NameResolver.BUILTIN_RANGE)),    //range
//                        Map.entry(5, new VariableCell(NameResolver.BUILTIN_INT)),    //int
//
//                        Map.entry(6, new VariableCell(new UndefinedValue())),  //let list
//                        Map.entry(7, new VariableCell(new UndefinedValue()))  //for->let i
//                ));
    }

    @Test
    void function() {
        nameResolution("""
                        func add(a: int, b: int) -> int {
                            return a + b;
                        }
                        let a: int = add(1, 2);
                        """,
                Map.ofEntries(
                        Map.entry(0, new VariableCell(NameResolver.BUILTIN_PRINT)),    //print
                        Map.entry(1, new VariableCell(NameResolver.BUILTIN_INPUT)),    //input
                        Map.entry(2, new VariableCell(NameResolver.BUILTIN_STRING)),    //string
                        Map.entry(3, new VariableCell(NameResolver.BUILTIN_LENGTH)),    //length
                        Map.entry(4, new VariableCell(NameResolver.BUILTIN_RANGE)),    //range
                        Map.entry(5, new VariableCell(NameResolver.BUILTIN_INT)),    //int

                        Map.entry(6, new VariableCell(new UndefinedValue())),  //func add
                        Map.entry(7, new VariableCell(new UndefinedValue())),  //add->a
                        Map.entry(8, new VariableCell(new UndefinedValue())),  //add->b

                        Map.entry(9, new VariableCell(new UndefinedValue()))  //let a
                ));
    }

    @Test
    void nestedFunction() {
        nameResolution("""
                        func add(a: int, b: int) -> int {
                            func impl(a: int, b: int) -> int {
                                return a + b;
                            }
                            return impl(a, b);
                        }
                        
                        let b: int = add(3, 4);
                        """,
                Map.ofEntries(
                        Map.entry(0, new VariableCell(NameResolver.BUILTIN_PRINT)),    //print
                        Map.entry(1, new VariableCell(NameResolver.BUILTIN_INPUT)),    //input
                        Map.entry(2, new VariableCell(NameResolver.BUILTIN_STRING)),    //string
                        Map.entry(3, new VariableCell(NameResolver.BUILTIN_LENGTH)),    //length
                        Map.entry(4, new VariableCell(NameResolver.BUILTIN_RANGE)),    //range
                        Map.entry(5, new VariableCell(NameResolver.BUILTIN_INT)),    //int

                        Map.entry(6, new VariableCell(new UndefinedValue())),  //func add
                        Map.entry(7, new VariableCell(new UndefinedValue())),  //add->a
                        Map.entry(8, new VariableCell(new UndefinedValue())),  //add->b

                        Map.entry(9, new VariableCell(new UndefinedValue())),  //add->func impl
                        Map.entry(10, new VariableCell(new UndefinedValue())),  //add->impl->a
                        Map.entry(11, new VariableCell(new UndefinedValue())),  //add->impl->b

                        Map.entry(12, new VariableCell(new UndefinedValue()))   //let b
                ));
    }
}
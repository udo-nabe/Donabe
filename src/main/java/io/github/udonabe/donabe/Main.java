package io.github.udonabe.donabe;

import io.github.udonabe.donabe.ast.ASTViewer;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.BinaryOperator;
import io.github.udonabe.donabe.ast.expr.UnaryOperator;
import io.github.udonabe.donabe.runtime.Interpreter;
import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.OperationRegistry;
import io.github.udonabe.donabe.runtime.value.*;
import io.github.udonabe.donabe.lexer.Lexer;
import io.github.udonabe.donabe.parser.BasicParsers;
import io.github.udonabe.donabe.parser.ParseFailed;
import io.github.udonabe.donabe.parser.ParseResult;
import io.github.udonabe.donabe.parser.ParseSuccess;
import io.github.udonabe.donabe.semantic.SemanticAnalyzer;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Main {
    private static void setupOutput() {
        System.setOut(new PrintStream(
                System.out,
                true,
                StandardCharsets.UTF_8
        ));
        System.setErr(new PrintStream(
                System.err,
                true,
                StandardCharsets.UTF_8
        ));
    }

    public static void main(String[] args) throws IllegalAccessException {
        setupOutput();
        try (Reader reader = new BufferedReader(new FileReader(args[0]))) {
            StringBuilder source = new StringBuilder();
            for (int ch = reader.read();
                 ch != -1;
                 ch = reader.read()) {
                source.append((char) ch);
            }

            Lexer lexer = new Lexer(source.toString());
            TokenStream stream = lexer.toTokenStream();
            TokenStream st = stream.fork();
//            while (!st.isAtEnd()) {
//                System.out.println(st.advance());
//            }

            var parser = BasicParsers.program;
            ParseResult<Program> result = parser.parse(stream);

            if (result instanceof ParseFailed<Program>(String message, int ignored)) throw new CompileException(message);

            Program parsed = ((ParseSuccess<Program>) result).value();
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source.toString());
            semanticAnalyzer.check(parsed);
            ASTViewer.view(parsed, System.out);

            OperationRegistry registry = generateRegistry();

            Interpreter interpreter = new Interpreter(parsed, registry, source.toString());
            interpreter.run();
        } catch (CompileException e) {
            System.err.println("コンパイルエラー: " + e.getMessage());
            System.exit(1);
        } catch (InterpreterException e) {
            System.err.println("実行時エラー: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static OperationRegistry generateRegistry() {
        OperationRegistry registry = new OperationRegistry();
        // int | int
        registry.registerBinary(BinaryOperator.PLUS, IntegerValue.class, IntegerValue.class, (l, r) -> new IntegerValue(l.value() + r.value()));
        registry.registerBinary(BinaryOperator.MINUS, IntegerValue.class, IntegerValue.class, (l, r) -> new IntegerValue(l.value() - r.value()));
        registry.registerBinary(BinaryOperator.MULTIPLICATION, IntegerValue.class, IntegerValue.class, (l, r) -> new IntegerValue(l.value() * r.value()));
        registry.registerBinary(BinaryOperator.DIVISION, IntegerValue.class, IntegerValue.class, (l, r) -> new IntegerValue(l.value() / r.value()));

        // string | string
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, StringValue.class, (l, r) -> new StringValue(l.value() + r.value()));

        // string-related
        registry.registerBinary(BinaryOperator.PLUS, IntegerValue.class, StringValue.class, (l, r) -> new StringValue(l.display() + r.display()));
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, IntegerValue.class, (l, r) -> new StringValue(l.display() + r.display()));

        registry.registerBinary(BinaryOperator.PLUS, BooleanValue.class, StringValue.class, (l, r) -> new StringValue(l.display() + r.display()));
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, BooleanValue.class, (l, r) -> new StringValue(l.display() + r.display()));

        registry.registerBinary(BinaryOperator.PLUS, FunctionValue.class, StringValue.class, (l, r) -> new StringValue(l.display() + r.display()));
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, FunctionValue.class, (l, r) -> new StringValue(l.display() + r.display()));

        registry.registerBinary(BinaryOperator.PLUS, ListValue.class, StringValue.class, (l, r) -> new StringValue(l.display() + r.display()));
        registry.registerBinary(BinaryOperator.PLUS, StringValue.class, ListValue.class, (l, r) -> new StringValue(l.display() + r.display()));

        // Equal
        registry.registerBinary(BinaryOperator.EQUAL, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.equals(r)));
        registry.registerBinary(BinaryOperator.EQUAL, StringValue.class, StringValue.class, (l, r) -> new BooleanValue(l.equals(r)));
        registry.registerBinary(BinaryOperator.EQUAL, BooleanValue.class, BooleanValue.class, (l, r) -> new BooleanValue(l.equals(r)));

        // Compare
        registry.registerBinary(BinaryOperator.LESS, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.value() > r.value()));
        registry.registerBinary(BinaryOperator.GREATER, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.value() < r.value()));
        registry.registerBinary(BinaryOperator.LESS_EQUAL, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.value() >= r.value()));
        registry.registerBinary(BinaryOperator.GREATER_EQUAL, IntegerValue.class, IntegerValue.class, (l, r) -> new BooleanValue(l.value() <= r.value()));

        // Unary
        registry.registerUnary(UnaryOperator.PLUS, IntegerValue.class, t -> new IntegerValue(t.value()));
        registry.registerUnary(UnaryOperator.MINUS, IntegerValue.class, t -> new IntegerValue(-t.value()));
        registry.registerUnary(UnaryOperator.NOT, BooleanValue.class, t -> new BooleanValue(!t.value()));

        return registry;
    }
}

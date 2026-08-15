package io.github.udonabe.donabe;

import io.github.udonabe.donabe.ast.ASTViewer;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ast.expr.BinaryOperator;
import io.github.udonabe.donabe.ast.expr.UnaryOperator;
import io.github.udonabe.donabe.lexer.Lexer;
import io.github.udonabe.donabe.parser.BasicParsers;
import io.github.udonabe.donabe.parser.ParseFailed;
import io.github.udonabe.donabe.parser.ParseResult;
import io.github.udonabe.donabe.parser.ParseSuccess;
import io.github.udonabe.donabe.runtime.Interpreter;
import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.OperationRegistry;
import io.github.udonabe.donabe.runtime.value.*;
import io.github.udonabe.donabe.semantic.SemanticAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "donabe",
        version = "1.0-SNAPSHOT",
        description = "Donabe言語 処理系",
        mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static {
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

    @CommandLine.Parameters(index = "0",
            description = "ソースファイル",
            paramLabel = "<file>")
    Path sourceFile;
    @CommandLine.Option(
            names = {"--verbose"},
            description = "ログを詳細表示するか"
    )
    boolean verbose;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        LoggingUtil.configure(verbose);
        log.info("Donabe launched.");
        try (Reader reader = Files.newBufferedReader(sourceFile)) {
            StringBuilder source = new StringBuilder();
            for (int ch = reader.read();
                 ch != -1;
                 ch = reader.read()) {
                source.append((char) ch);
            }
            log.debug("Source file read.");
            log.trace("Source: {}{}", System.lineSeparator(), source);

            Lexer lexer = new Lexer(source.toString());
            TokenStream stream = lexer.toTokenStream();
            log.debug("Lexical analysis successful.");
            log.trace("Tokens: {}", stream);

            var parser = BasicParsers.program;
            ParseResult<Program> result = parser.parse(stream);

            if (result instanceof ParseFailed<Program>(String message, int ignored))
                throw new CompileException(message);

            Program parsed = ((ParseSuccess<Program>) result).value();
            log.debug("Parse successful.");

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source.toString());
            var variables = semanticAnalyzer.check(parsed);
            log.debug("Semantic analysis successful.");

            ASTViewer.view(parsed, System.out);

            OperationRegistry registry = generateRegistry();

            Interpreter interpreter = new Interpreter(parsed, registry, source.toString(), variables);
            log.debug("Launching interpreter...");
            interpreter.run();
            log.info("Normal termination.");
        } catch (CompileException e) {
            System.err.println("コンパイルエラー: " + e.getMessage());
            return 1;
        } catch (InterpreterException e) {
            System.err.println("実行時エラー: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            log.error("An internal error has occurred.", e);
            return 1;
        }
        return 0;
    }

    private OperationRegistry generateRegistry() {
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

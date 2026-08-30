package io.github.udonabe.donabe;

import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.ir.IRViewer;
import io.github.udonabe.donabe.lexer.Lexer;
import io.github.udonabe.donabe.parser.*;
import io.github.udonabe.donabe.runtime.IRInterpreter;
import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.Operations;
import io.github.udonabe.donabe.semantic.SemanticAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        try (Reader reader = Files.newBufferedReader(sourceFile)) {
            LoggingUtil.configure(verbose);
            log.info("Donabe launched.");

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

            Parser<Program> parser = BasicParsers.program;
            ParseResult<Program> result = parser.parse(stream);

            if (result instanceof ParseFailed<Program>(String message, int ignored)) {
                    throw new CompileException(message);
            }

            Program parsed = ((ParseSuccess<Program>) result).value();
            log.debug("Parse successful.");

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source.toString());
            SemanticAnalyzer.AnalyzeResult checkResult = semanticAnalyzer.check(parsed);
            log.debug("Semantic analysis successful.");
            log.debug("IR: \n{}", new IRViewer().getIRString(checkResult.irProgram()));

            log.debug("Launching interpreter...");

            Operations registry = new Operations();
            IRInterpreter interpreter = new IRInterpreter(checkResult.irProgram(), checkResult.resolution(), registry);
            interpreter.run();

            log.info("Normal termination.");
        } catch (CompileException e) {
            log.warn("Compile error.", e);
            System.err.println("Compile error: " + e.getMessage());
            return 1;
        } catch (InterpreterException e) {
            log.warn("Runtime error.", e);
            System.err.println("Runtime error: " + e.getMessage());
            return 1;
        } catch (Exception | AssertionError e) {
            log.error("An internal error has occurred.", e);
            return 1;
        } catch (Throwable e) {
            e.printStackTrace();    //ロギングすら失敗する可能性があるため、System.errにスタックトレースを出す
            System.exit(1);
        }
        return 0;
    }
}

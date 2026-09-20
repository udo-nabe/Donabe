package io.github.udonabe.donabe;

import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.compile.Compiler;
import io.github.udonabe.donabe.compile.Encoder;
import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.error.ErrorUtil;
import io.github.udonabe.donabe.ir.IRViewer;
import io.github.udonabe.donabe.lexer.Lexer;
import io.github.udonabe.donabe.parser.*;
import io.github.udonabe.donabe.runtime.IRInterpreter;
import io.github.udonabe.donabe.runtime.InterpreterException;
import io.github.udonabe.donabe.runtime.Operations;
import io.github.udonabe.donabe.semantic.SemanticAnalyzer;
import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

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
    @CommandLine.Option(
            names = {"--run"},
            description = "ログを詳細表示するか"
    )
    boolean isRun;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try {
            LoggingUtil.configure(verbose);
            log.info("Donabe launched.");

            String source = Files.readString(sourceFile, StandardCharsets.UTF_8);

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

            if (isRun) {
                log.debug("Launching interpreter...");

                Operations registry = new Operations();
                Set<Integer> slots = Set.copyOf(
                        IntStream.range(0, checkResult.resolutionMax())
                                .mapToObj(i -> i)
                                .toList()
                );
                IRInterpreter interpreter = new IRInterpreter(checkResult.irProgram(), slots, registry);
                interpreter.run();

                log.info("Normal termination.");
            } else {
                log.debug("Compiling...");

                Compiler compiler = new Compiler();
                ByteCode code = compiler.compile(checkResult.irProgram(), checkResult.resolutionMax());

                log.debug("Success to compile.");
                log.debug("Encoding...");

                Encoder encoder = new Encoder();
                byte[] encoded = encoder.encode(code);

                log.debug("Success to encode.");
                log.debug("Write to file...");

                writeFile(encoded);
            }
        } catch (CompileException e) {
            log.warn("Compile error.", e);
            System.err.println("Compile error: " + e.getMessage());
            return 1;
        } catch (InterpreterException e) {
            String msg = ErrorUtil.makeRuntimeError(e.occurredFrame(), e.getMessage());
            log.warn("Runtime error.", e);
            System.err.println("Runtime error: " + msg);
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

    private void writeFile(byte[] encoded) throws IOException {
        Path outPath = Path.of("test.dnbc");

        try (OutputStream out = Files.newOutputStream(outPath)) {
            out.write(encoded);
            out.flush();
        }
    }
}

package io.github.udonabe.donabe;

import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.compile.Compiler;
import io.github.udonabe.donabe.compile.Encoder;
import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.error.ErrorUtil;
import io.github.udonabe.donabe.ir.IRViewer;
import io.github.udonabe.donabe.lexer.Lexer;
import io.github.udonabe.donabe.parser.*;
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
        description = "Donabe compiler",
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
            description = "Source file",
            paramLabel = "<file>")
    Path sourceFile;
    @CommandLine.Option(
            names = {"--verbose"},
            description = "Enable verbose logging"
    )
    boolean verbose;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try {
            LoggingUtil.configure(verbose);
            log.info("Donabe launched.");
            
            if (!sourceFile.toString().endsWith("\\.dnb")) {
                System.err.println("Error: Source file must have a .dnb extension.");
                return 1;
            }

            String source = Files.readString(sourceFile, StandardCharsets.UTF_8);

            log.debug("Source file read.");
            log.trace("Source: {}{}", System.lineSeparator(), source);

            Lexer lexer = new Lexer(source);
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

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source);
            SemanticAnalyzer.AnalyzeResult checkResult = semanticAnalyzer.check(parsed);
            log.debug("Semantic analysis successful.");
            log.trace("IR: \n{}", new IRViewer().getIRString(checkResult.irProgram()));

            log.debug("Compiling.");

            Compiler compiler = new Compiler();
            ByteCode code = compiler.compile(checkResult.irProgram(), checkResult.globals());

            log.debug("Compilation successful.");
            log.debug("Encoding.");

            Encoder encoder = new Encoder();
            byte[] encoded = encoder.encode(code);

            log.debug("Encoding successful.");
            log.debug("Writing to file.");

            writeFile(sourceFile, encoded);
            
            log.debug("Writing successful.");
            log.info("Normal termination.");
            return 0;
        } catch (IOException e) {
            log.warn("I/O error.", e);
            System.err.println("I/O error occurred: " + e.getMessage());
            return 1;
        } catch (CompileException e) {
            log.warn("Compile error.", e);
            System.err.println("Error: " + e.getMessage());
            return 1;
        }  catch (Exception | AssertionError e) {
            log.error("An internal error has occurred.", e);
            return 1;
        }
    }

    private void writeFile(Path source, byte[] encoded) throws IOException {
        //拡張子.dnbを.dnbcへ書き換える
        String sourceFilename = source.getFileName().toString();
        Path outPath = source.resolveSibling(
                sourceFilename.substring(0, sourceFilename.length() - 4) + ".dnbc"
        );
        log.debug("Output file: {}", outPath);
        
        try (OutputStream out = Files.newOutputStream(outPath)) {
            log.debug("Writing {} bytes.", Integer.toHexString(encoded.length));
            out.write(encoded);
            out.flush();
        }
    }
}

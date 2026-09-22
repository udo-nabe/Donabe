package io.github.udonabe.donabe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;
import io.github.udonabe.donabe.ast.Program;
import io.github.udonabe.donabe.compile.Compiler;
import io.github.udonabe.donabe.compile.code.ByteCode;
import io.github.udonabe.donabe.compile.code.constant.ConstantPoolEntry;
import io.github.udonabe.donabe.compile.code.section.ConstantPoolSection;
import io.github.udonabe.donabe.lexer.Lexer;
import io.github.udonabe.donabe.parser.BasicParsers;
import io.github.udonabe.donabe.parser.ParseFailed;
import io.github.udonabe.donabe.parser.ParseResult;
import io.github.udonabe.donabe.parser.ParseSuccess;
import io.github.udonabe.donabe.parser.Parser;
import io.github.udonabe.donabe.semantic.SemanticAnalyzer;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    @Test
    void integrationTests() throws URISyntaxException, IOException {
        Path root = Path.of(getClass().getResource("/integration").toURI());
        System.out.println("root: " + root);

        try (var paths = Files.walk(root)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".dnb"))
                    .forEach(p -> {
                        System.out.println("TEST: " + p);
                        integrationTest(p);
                    });
        }
    }

    void integrationTest(Path dnbFile) {
        Path expectFile = dnbFile.resolveSibling(
                dnbFile.getFileName().toString().replaceFirst("\\.[^.]+$", ".dump")
        );

        if (!Files.exists(expectFile)) {
            throw new RuntimeException(".dump file not found.");
        }

        try {
            String source = Files.readString(dnbFile);
            String expected = Files.readString(expectFile);

            Lexer lexer = new Lexer(source.toString());
            TokenStream stream = lexer.toTokenStream();

            Parser<Program> parser = BasicParsers.program;
            ParseResult<Program> result = parser.parse(stream);

            if (result instanceof ParseFailed<Program>(String message, int ignored)) {
                fail("Failed to parse source. \n" + message);
            }

            Program parsed = ((ParseSuccess<Program>) result).value();

            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(source.toString());
            SemanticAnalyzer.AnalyzeResult checkResult = semanticAnalyzer.check(parsed);

            ByteCode code = new Compiler().compile(checkResult.irProgram(), checkResult.globals());

            
            String acutalDump = ByteCodeDumper.dump(code).strip();
            String expectedDump = expected.strip();
            assertEquals(expectedDump, acutalDump);
        } catch (IOException e) {
            throw new RuntimeException("Failed to run test.", e);
        }
    }
}

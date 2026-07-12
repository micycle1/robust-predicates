package com.github.micycle1.robustpredicates.codegen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the checked-in generated file against drift: regenerating in memory
 * must reproduce it exactly. If this fails, run
 * {@code mvn -Pcodegen compile exec:java} and commit the result.
 */
class CodegenDriftTest {

    @Test
    void checkedInFileMatchesRegeneration() throws IOException {
        // Surefire runs with the working directory at the project basedir.
        Path file = Path.of("src", "main", "java", "com", "github", "micycle1", "robustpredicates", "generated",
                "RobustPredicates.java");
        String checkedIn = Files.readString(file).replace("\r\n", "\n");
        String regenerated = CodeGenerator.generateDefault().replace("\r\n", "\n");
        assertEquals(regenerated, checkedIn,
                "generated file is stale; rerun: mvn -Pcodegen compile exec:java");
    }

    @Test
    void generationIsDeterministic() {
        assertEquals(CodeGenerator.generateDefault(), CodeGenerator.generateDefault());
    }
}

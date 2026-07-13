package com.github.micycle1.robustpredicates.codegen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the checked-in generated file against drift: regenerating in memory
 * must reproduce it exactly. If this fails, run
 * {@code mvn -Pcodegen -pl framework -am process-classes} and commit the
 * result.
 */
class CodegenDriftTest {

    @Test
    void checkedInFileMatchesRegeneration() throws IOException {
        // Surefire runs with the working directory at the framework module basedir.
        Path file = Path.of("..", "predicates", "src", "main", "java",
                "com", "github", "micycle1", "robustpredicates", "RobustPredicates.java");
        String checkedIn = Files.readString(file).replace("\r\n", "\n");
        String regenerated = CodeGenerator.generateDefault().replace("\r\n", "\n");
        assertEquals(regenerated, checkedIn,
                "generated file is stale; rerun: mvn -Pcodegen -pl framework -am process-classes");
    }

    @Test
    void generationIsDeterministic() {
        assertEquals(CodeGenerator.generateDefault(), CodeGenerator.generateDefault());
    }
}

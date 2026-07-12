package com.github.micycle1.robustpredicates.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the generated {@code com.github.micycle1.robustpredicates.generated.RobustPredicates} source file.
 * Run from the project root (or via {@code mvn -Pcodegen compile exec:java});
 * an optional first argument overrides the source root (default
 * {@code src/main/java}).
 */
public final class GenerateMain {

    private GenerateMain() {
    }

    public static void main(String[] args) throws IOException {
        Path sourceRoot = Path.of(args.length > 0 ? args[0] : "src/main/java");
        Path file = sourceRoot.resolve(Path.of("com", "github", "micycle1", "robustpredicates", "generated", "RobustPredicates.java"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, CodeGenerator.generateDefault());
        System.out.println("Wrote " + file.toAbsolutePath());
    }
}

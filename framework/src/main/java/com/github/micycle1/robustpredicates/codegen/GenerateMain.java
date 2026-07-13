package com.github.micycle1.robustpredicates.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the generated {@code RobustPredicates} source file into the
 * {@code predicates} module. Run via
 * {@code mvn -Pcodegen -pl framework -am process-classes}; an optional first
 * argument overrides the target source root (default
 * {@code ../predicates/src/main/java}, relative to the {@code framework}
 * module directory).
 */
public final class GenerateMain {

    private GenerateMain() {
    }

    public static void main(String[] args) throws IOException {
        Path sourceRoot = Path.of(args.length > 0 ? args[0] : "../predicates/src/main/java");
        Path file = sourceRoot.resolve(
                Path.of("com", "github", "micycle1", "robustpredicates", "RobustPredicates.java"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, CodeGenerator.generateDefault());
        System.out.println("Wrote " + file.toAbsolutePath().normalize());
    }
}

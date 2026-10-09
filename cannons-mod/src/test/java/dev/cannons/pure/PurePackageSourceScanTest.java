package dev.cannons.pure;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PurePackageSourceScanTest {
    @Test
    void pureSourcePackagesNeverImportMinecraft() throws Exception {
        Path sourceRoot = Path.of("src", "main", "java", "dev", "cannons", "pure");
        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("import net.minecraft"),
                        () -> "Pure source imports net.minecraft: " + file);
            }
        }
    }
}

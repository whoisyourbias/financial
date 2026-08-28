package dev.whoisyourbias.financial.harness;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("module-boundary-functional")
class ModuleBoundaryFunctionalTest {

  private static final List<String> BUILD_FIXTURE_FILES =
      List.of(
          "settings.gradle.kts",
          "settings-gradle.lockfile",
          "build.gradle.kts",
          "gradle.properties",
          "gradle/libs.versions.toml",
          "gradle/verification-metadata.xml",
          "gradle/verification-keyring.gpg",
          "gradle/verification-keyring.keys",
          "gradle/harness.gradle.kts",
          "platform/bootstrap/build.gradle.kts",
          "platform/ledger/build.gradle.kts",
          "platform/shared-kernel/build.gradle.kts",
          "tools/knowledge-harness/build.gradle.kts");

  @TempDir Path temporaryDirectory;

  @Test
  void rejectsAReverseProjectDependency() throws IOException {
    Path repositoryRoot = repositoryRoot();
    for (String relativePath : BUILD_FIXTURE_FILES) {
      copy(repositoryRoot, temporaryDirectory, relativePath);
    }
    Path initScript = temporaryDirectory.resolve("forbidden-dependency.init.gradle");
    Files.writeString(
        initScript,
        """
        gradle.beforeProject { candidate ->
            if (candidate.path == ':platform:shared-kernel') {
                candidate.pluginManager.withPlugin('java-library') {
                    candidate.dependencies.add(
                        'implementation',
                        candidate.project(':platform:ledger')
                    )
                }
            }
        }
        """,
        StandardCharsets.UTF_8);

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(temporaryDirectory.toFile())
            .withArguments(
                "--init-script",
                initScript.toString(),
                "moduleBoundaryCheck",
                "--no-configuration-cache")
            .buildAndFail();

    assertTrue(
        result.getOutput().contains(":platform:shared-kernel -> :platform:ledger (implementation)"),
        result.getOutput());
  }

  @Test
  void rejectsDirectAccessToLedgerPersistenceTypes() throws IOException {
    Path ledgerJar =
        Files.list(repositoryRoot().resolve("platform/ledger/build/libs"))
            .filter(path -> path.getFileName().toString().endsWith(".jar"))
            .findFirst()
            .orElseThrow();
    Files.writeString(
        temporaryDirectory.resolve("settings.gradle"),
        "rootProject.name = 'illegal-ledger-consumer'\n",
        StandardCharsets.UTF_8);
    String escapedJarPath =
        ledgerJar.toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "\\'");
    Files.writeString(
        temporaryDirectory.resolve("build.gradle"),
        "plugins { id 'java' }\n"
            + "dependencies { implementation files('"
            + escapedJarPath
            + "') }\n",
        StandardCharsets.UTF_8);
    Path source =
        temporaryDirectory.resolve(
            "src/main/java/dev/whoisyourbias/financial/consumer/IllegalPersistenceAccess.java");
    Files.createDirectories(source.getParent());
    Files.writeString(
        source,
        """
        package dev.whoisyourbias.financial.consumer;

        import dev.whoisyourbias.financial.ledger.LedgerAccountEntity;
        import dev.whoisyourbias.financial.ledger.LedgerAccountRepository;

        final class IllegalPersistenceAccess {
          private LedgerAccountEntity entity;
          private LedgerAccountRepository repository;
        }
        """,
        StandardCharsets.UTF_8);

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(temporaryDirectory.toFile())
            .withArguments("compileJava", "--no-configuration-cache")
            .buildAndFail();

    assertTrue(
        result.getOutput().contains("is not public in dev.whoisyourbias.financial.ledger"),
        result.getOutput());
  }

  private static Path repositoryRoot() {
    return Path.of(System.getProperty("financial.repository.root"));
  }

  private static void copy(Path sourceRoot, Path targetRoot, String relativePath)
      throws IOException {
    Path source = sourceRoot.resolve(relativePath);
    Path target = targetRoot.resolve(relativePath);
    Files.createDirectories(target.getParent());
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
  }
}

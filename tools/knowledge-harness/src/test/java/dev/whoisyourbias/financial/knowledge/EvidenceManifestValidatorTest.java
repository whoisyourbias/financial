package dev.whoisyourbias.financial.knowledge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvidenceManifestValidatorTest {

  @TempDir Path repository;

  @Test
  void acceptsSchemaArtifactsAndAllowedEvidenceOnlyDiff() throws Exception {
    prepareCandidate(false);

    assertDoesNotThrow(() -> new EvidenceManifestValidator(repository).check("01-ledger-core"));
  }

  @Test
  void rejectsArtifactChecksumMismatch() throws Exception {
    Path artifact = prepareCandidate(false);
    Files.writeString(artifact, "tampered\n", StandardCharsets.UTF_8);

    EvidenceManifestValidator.EvidenceValidationException failure =
        assertThrows(
            EvidenceManifestValidator.EvidenceValidationException.class,
            () -> new EvidenceManifestValidator(repository).check("01-ledger-core"));

    assertTrue(failure.getMessage().contains("checksum mismatch"));
  }

  @Test
  void rejectsChangesOutsideEvidenceAllowlist() throws Exception {
    prepareCandidate(true);

    EvidenceManifestValidator.EvidenceValidationException failure =
        assertThrows(
            EvidenceManifestValidator.EvidenceValidationException.class,
            () -> new EvidenceManifestValidator(repository).check("01-ledger-core"));

    assertTrue(
        failure.getMessage().contains("change outside evidence allowlist: build.gradle.kts"));
  }

  @Test
  void rejectsMissingRequiredHumanReviewSection() throws Exception {
    prepareCandidate(false);
    Path manifest = repository.resolve("projects/01-ledger-core/evidence/MANIFEST.md");
    String withoutLimitations =
        Files.readString(manifest, StandardCharsets.UTF_8).replace("## Limitations\n", "");
    Files.writeString(manifest, withoutLimitations, StandardCharsets.UTF_8);

    EvidenceManifestValidator.EvidenceValidationException failure =
        assertThrows(
            EvidenceManifestValidator.EvidenceValidationException.class,
            () -> new EvidenceManifestValidator(repository).check("01-ledger-core"));

    assertTrue(failure.getMessage().contains("required H2 must appear exactly once: Limitations"));
  }

  @Test
  void acceptsFinalAuditOnlyWhenAllTwelveProjectManifestsAreLinked() throws Exception {
    prepareFinalAuditCandidate();

    assertDoesNotThrow(() -> new EvidenceManifestValidator(repository).check("final-audit"));
  }

  private Path prepareCandidate(boolean includeForbiddenChange) throws Exception {
    git("init", "-b", "develop");
    git("config", "user.name", "Evidence Test");
    git("config", "user.email", "evidence@example.test");
    Path config = repository.resolve("gradle/libs.versions.toml");
    Files.createDirectories(config.getParent());
    Files.writeString(config, "[versions]\n", StandardCharsets.UTF_8);
    Path project = repository.resolve("projects/01-ledger-core");
    Files.createDirectories(project);
    Files.writeString(project.resolve("PLAN.md"), "# Plan\n", StandardCharsets.UTF_8);
    git("add", ".");
    git("commit", "-m", "source");
    String sourceSha = git("rev-parse", "HEAD");

    Path evidence = project.resolve("evidence");
    Path artifact = evidence.resolve("raw/result.json");
    Files.createDirectories(artifact.getParent());
    Files.writeString(artifact, "{\"status\":\"PASS\"}\n", StandardCharsets.UTF_8);
    Files.writeString(
        evidence.resolve("results.md"),
        "# Results\n\n## Summary\n\nPASS\n",
        StandardCharsets.UTF_8);
    Files.writeString(
        evidence.resolve("MANIFEST.md"),
        validManifest(sourceSha, sha256(artifact)),
        StandardCharsets.UTF_8);
    if (includeForbiddenChange) {
      Files.writeString(
          repository.resolve("build.gradle.kts"), "plugins { java }\n", StandardCharsets.UTF_8);
    }
    git("add", ".");
    git("commit", "-m", "evidence candidate");
    return artifact;
  }

  private static String validManifest(String sourceSha, String artifactSha) {
    return """
        ---
        schemaVersion: 1
        projectId: 01-ledger-core
        sourceSha: %s
        generatedAt: 2026-08-30T00:00:00Z
        intendedTags:
          - p01-ledger-core
        environment:
          os: test-os
          cpu: test-cpu
          memoryBytes: 1024
          jvm: temurin-21
          dependencies: [postgresql-18.6]
        commands:
          - command: ./gradlew harnessFull
            configPaths: [gradle/libs.versions.toml]
        dataset:
          seed: "42"
          size: 1000
          distribution: fixed-fixture-v1
          version: "1"
        datasetNotApplicableReason: null
        aiContext: null
        aiNotApplicableReason: non-AI project
        measurement:
          warmupSeconds: 30
          durationSeconds: 300
          concurrency: 8
          requestMix: fixed-mix-v1
        measurementNotApplicableReason: null
        artifacts:
          - path: raw/result.json
            sha256: %s
            summaryRef: results.md#summary
        limitations:
          - synthetic data only
        ---

        # Evidence

        ## Environment

        Test environment.

        ## Commands

        Full harness.

        ## Dataset

        Fixed fixture.

        ## AI Context

        Not applicable.

        ## Artifacts

        Raw result and summary.

        ## Limitations

        Synthetic only.
        """
        .formatted(sourceSha, artifactSha);
  }

  private void prepareFinalAuditCandidate() throws Exception {
    git("init", "-b", "develop");
    git("config", "user.name", "Evidence Test");
    git("config", "user.email", "evidence@example.test");
    Path config = repository.resolve("gradle/libs.versions.toml");
    Files.createDirectories(config.getParent());
    Files.writeString(config, "[versions]\n", StandardCharsets.UTF_8);
    List<Path> projectManifests = new ArrayList<>();
    for (int index = 1; index <= 12; index++) {
      String projectId = "%02d-project".formatted(index);
      Path project = repository.resolve("projects").resolve(projectId);
      Files.createDirectories(project);
      Files.writeString(project.resolve("PLAN.md"), "# Plan\n", StandardCharsets.UTF_8);
      projectManifests.add(project.resolve("evidence/MANIFEST.md"));
    }
    git("add", ".");
    git("commit", "-m", "source");
    String sourceSha = git("rev-parse", "HEAD");

    for (Path projectManifest : projectManifests) {
      Files.createDirectories(projectManifest.getParent());
      Files.writeString(
          projectManifest,
          "project manifest " + projectManifest.getParent().getParent().getFileName() + "\n",
          StandardCharsets.UTF_8);
    }
    Path evidence = repository.resolve("reviews/evidence");
    Files.createDirectories(evidence);
    Files.writeString(
        evidence.resolve("results.md"),
        "# Final results\n\n## Summary\n\nPASS\n",
        StandardCharsets.UTF_8);
    StringBuilder artifacts = new StringBuilder();
    for (Path projectManifest : projectManifests) {
      String relative = evidence.relativize(projectManifest).toString().replace('\\', '/');
      artifacts
          .append("  - path: ")
          .append(relative)
          .append("\n    sha256: ")
          .append(sha256(projectManifest))
          .append("\n    summaryRef: results.md#summary\n");
    }
    Files.writeString(
        evidence.resolve("MANIFEST.md"),
        finalAuditManifest(sourceSha, artifacts.toString()),
        StandardCharsets.UTF_8);
    git("add", ".");
    git("commit", "-m", "final evidence candidate");
  }

  private static String finalAuditManifest(String sourceSha, String artifacts) {
    return """
        ---
        schemaVersion: 1
        projectId: final-audit
        sourceSha: %s
        generatedAt: 2026-08-30T00:00:00Z
        intendedTags: [showcase-02-ai-payment-ops]
        environment:
          os: test-os
          cpu: test-cpu
          memoryBytes: 1024
          jvm: temurin-21
          dependencies: [postgresql-18.6]
        commands:
          - command: ./gradlew harnessFull
            configPaths: [gradle/libs.versions.toml]
        dataset: null
        datasetNotApplicableReason: aggregate project evidence
        aiContext:
          provider: local
          model: test-model
          modelVersion: "1"
          promptSha256: null
          promptNotApplicableReason: deterministic aggregate validator
          corpusVersion: synthetic-dataset-v1
          corpusNotApplicableReason: null
        aiNotApplicableReason: null
        measurement: null
        measurementNotApplicableReason: aggregate project evidence
        artifacts:
        %s
        limitations: [synthetic data only]
        ---

        # Final Evidence

        ## Environment

        Test environment.

        ## Commands

        Full harness.

        ## Dataset

        Aggregate evidence.

        ## AI Context

        Local deterministic context.

        ## Artifacts

        Twelve project manifests.

        ## Limitations

        Synthetic only.
        """
        .formatted(sourceSha, artifacts.stripTrailing());
  }

  private String git(String... arguments) throws IOException, InterruptedException {
    String[] command = new String[arguments.length + 1];
    command[0] = "git";
    System.arraycopy(arguments, 0, command, 1, arguments.length);
    Process process =
        new ProcessBuilder(command)
            .directory(repository.toFile())
            .redirectErrorStream(true)
            .start();
    String output =
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IllegalStateException("git command failed: " + output);
    }
    return output;
  }

  private static String sha256(Path file) throws Exception {
    return HexFormat.of()
        .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
  }
}

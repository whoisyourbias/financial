package dev.whoisyourbias.financial.knowledge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KnowledgeHarnessTest {

  @TempDir Path temporaryDirectory;

  @Test
  void exportsIdenticalBytesForEquivalentCrLfAndLfInputs() throws Exception {
    Path document = prepareRepository("planned", false);
    KnowledgeHarness harness = new KnowledgeHarness(temporaryDirectory);

    Path firstOutput = harness.export();
    byte[] first = Files.readAllBytes(firstOutput);

    String withCrLf = Files.readString(document, StandardCharsets.UTF_8).replace("\n", "\r\n");
    Files.writeString(document, withCrLf, StandardCharsets.UTF_8);
    Path secondOutput = harness.export();

    assertArrayEquals(first, Files.readAllBytes(secondOutput));
  }

  @Test
  void keepsOversizedCodeAndTableBlocksIntact() throws Exception {
    prepareRepository("planned", false);
    KnowledgeHarness harness = new KnowledgeHarness(temporaryDirectory);

    String output = Files.readString(harness.export(), StandardCharsets.UTF_8);

    assertTrue(
        output
            .lines()
            .anyMatch(
                line ->
                    line.contains("\"oversized\":true")
                        && line.contains("```text")
                        && line.contains("END_OF_LONG_BLOCK")));
    assertTrue(
        output
            .lines()
            .anyMatch(
                line ->
                    line.contains("\"oversized\":true")
                        && line.contains("TABLE_START")
                        && line.contains("TABLE_END")));
  }

  @Test
  void rejectsVerifiedDocumentsWithoutSourceOrEvidence() throws Exception {
    prepareRepository("verified", false);
    KnowledgeHarness harness = new KnowledgeHarness(temporaryDirectory);

    KnowledgeHarness.KnowledgeValidationException failure =
        assertThrows(KnowledgeHarness.KnowledgeValidationException.class, harness::check);

    assertTrue(
        failure.getMessage().contains("verified document has no sourceRefs or evidenceRefs"));
  }

  @Test
  void rejectsBrokenHeadingAnchors() throws Exception {
    Path document = prepareRepository("planned", false);
    Files.writeString(
        document,
        Files.readString(document, StandardCharsets.UTF_8) + "\n[깨진 링크](sample.md#없는-제목)\n",
        StandardCharsets.UTF_8);
    KnowledgeHarness harness = new KnowledgeHarness(temporaryDirectory);

    KnowledgeHarness.KnowledgeValidationException failure =
        assertThrows(KnowledgeHarness.KnowledgeValidationException.class, harness::check);

    assertTrue(failure.getMessage().contains("broken heading anchor"));
  }

  @Test
  void rejectsUnsupportedStatuses() throws Exception {
    prepareRepository("published", false);
    KnowledgeHarness harness = new KnowledgeHarness(temporaryDirectory);

    KnowledgeHarness.KnowledgeValidationException failure =
        assertThrows(KnowledgeHarness.KnowledgeValidationException.class, harness::check);

    assertTrue(failure.getMessage().contains("unsupported status"));
  }

  @Test
  void createsUniqueIdsForSameNamedFilesAndRepeatedHeadings() throws Exception {
    Path domain = temporaryDirectory.resolve("docs/domain");
    Files.createDirectories(domain);
    Path first = temporaryDirectory.resolve("docs/first/same.md");
    Path second = temporaryDirectory.resolve("docs/second/same.md");
    Files.createDirectories(first.getParent());
    Files.createDirectories(second.getParent());
    Files.writeString(first, "# 반복\n\n첫째\n\n# 반복\n\n둘째\n", StandardCharsets.UTF_8);
    Files.writeString(second, "# 반복\n\n셋째\n", StandardCharsets.UTF_8);
    Files.writeString(
        domain.resolve("catalog.yaml"),
        """
        documents:
          - id: first-document
            title: First
            path: docs/first/same.md
            kind: domain
            domains: [ledger]
            status: planned
            version: "1.0"
            reviewedAt: 2026-08-28
            appliesTo: ["01"]
            sourceRefs: []
            evidenceRefs: []
          - id: second-document
            title: Second
            path: docs/second/same.md
            kind: domain
            domains: [ledger]
            status: planned
            version: "1.0"
            reviewedAt: 2026-08-28
            appliesTo: ["01"]
            sourceRefs: []
            evidenceRefs: []
        """,
        StandardCharsets.UTF_8);
    Files.writeString(domain.resolve("sources.yaml"), "sources: []\n", StandardCharsets.UTF_8);

    String output =
        Files.readString(new KnowledgeHarness(temporaryDirectory).export(), StandardCharsets.UTF_8);

    assertTrue(output.contains("\"id\":\"first-document-반복-1-1\""));
    assertTrue(output.contains("\"id\":\"first-document-반복-2-1\""));
    assertTrue(output.contains("\"id\":\"second-document-반복-1-1\""));
  }

  @Test
  void matchesTheCrossPlatformGoldenChecksum() throws Exception {
    prepareRepository("planned", false);

    byte[] output = Files.readAllBytes(new KnowledgeHarness(temporaryDirectory).export());
    String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(output));

    assertEquals("5cbeb19c9266f383444b15410aec8a1db011157dfaefdefae77831b5b8d1773b", checksum);
  }

  private Path prepareRepository(String status, boolean includeSource) throws Exception {
    Path domain = temporaryDirectory.resolve("docs/domain");
    Files.createDirectories(domain);
    Path document = temporaryDirectory.resolve("docs/sample.md");
    String longBlock = "x".repeat(1_700) + "END_OF_LONG_BLOCK";
    StringBuilder longTable = new StringBuilder("| key | value |\n| --- | --- |\n");
    for (int index = 0; index < 80; index++) {
      String marker = index == 0 ? "TABLE_START" : (index == 79 ? "TABLE_END" : "row-" + index);
      longTable.append("| ").append(marker).append(" | ").append("v".repeat(24)).append(" |\n");
    }
    Files.writeString(
        document,
        """
                # 샘플

                첫 문단입니다.

                ## 반복

                본문입니다.

                ## 반복

                ```text
                %s
                ```

                ## 긴 표

                %s
                """
            .formatted(longBlock, longTable),
        StandardCharsets.UTF_8);
    Files.writeString(
        domain.resolve("catalog.yaml"),
        """
                documents:
                  - id: sample
                    title: Sample
                    path: docs/sample.md
                    kind: domain
                    domains: [ledger]
                    status: %s
                    version: "1.0"
                    reviewedAt: 2026-08-28
                    appliesTo: ["01"]
                    sourceRefs: %s
                    evidenceRefs: []
                """
            .formatted(status, includeSource ? "[official]" : "[]"),
        StandardCharsets.UTF_8);
    Files.writeString(
        domain.resolve("sources.yaml"),
        includeSource
            ? """
                        sources:
                          - id: official
                            title: Official
                            url: https://example.com
                            publisher: Example
                            checkedAt: 2026-08-28
                            domains: [ledger]
                            scope: test
                            storesOriginal: false
                        """
            : "sources: []\n",
        StandardCharsets.UTF_8);
    return document;
  }
}

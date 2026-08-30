package dev.whoisyourbias.financial.knowledge;

import java.nio.file.Path;

public final class KnowledgeHarnessApplication {

  private KnowledgeHarnessApplication() {}

  public static void main(String[] arguments) throws Exception {
    if (arguments.length == 3 && "evidence-check".equals(arguments[0])) {
      EvidenceManifestValidator validator = new EvidenceManifestValidator(Path.of(arguments[1]));
      validator.check(arguments[2]);
      System.out.println("Evidence manifest PASS: " + arguments[2]);
      return;
    }
    if (arguments.length != 2) {
      throw new IllegalArgumentException(
          "Usage: <check|export> <repository-root> | evidence-check <repository-root> <project-id>");
    }
    KnowledgeHarness harness = new KnowledgeHarness(Path.of(arguments[1]));
    switch (arguments[0]) {
      case "check" -> harness.check();
      case "export" -> {
        Path output = harness.export();
        System.out.println("Knowledge export: " + output);
      }
      default -> throw new IllegalArgumentException("Unknown command: " + arguments[0]);
    }
  }
}

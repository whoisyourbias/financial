package dev.whoisyourbias.financial.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class EvidenceManifestValidator {

  private static final Pattern PROJECT_ID = Pattern.compile("\\d{2}-[a-z0-9-]+");
  private static final Pattern COMMIT_SHA = Pattern.compile("[0-9a-f]{40}");
  private static final Pattern CONTENT_SHA = Pattern.compile("[0-9a-f]{64}");
  private static final Set<String> REQUIRED_HEADINGS =
      Set.of("Environment", "Commands", "Dataset", "AI Context", "Artifacts", "Limitations");
  private static final Set<String> TOP_LEVEL_KEYS =
      Set.of(
          "schemaVersion",
          "projectId",
          "sourceSha",
          "generatedAt",
          "intendedTags",
          "environment",
          "commands",
          "dataset",
          "datasetNotApplicableReason",
          "aiContext",
          "aiNotApplicableReason",
          "measurement",
          "measurementNotApplicableReason",
          "artifacts",
          "limitations");
  private static final Set<String> ENVIRONMENT_KEYS =
      Set.of("os", "cpu", "memoryBytes", "jvm", "dependencies");
  private static final Set<String> COMMAND_KEYS = Set.of("command", "configPaths");
  private static final Set<String> DATASET_KEYS = Set.of("seed", "size", "distribution", "version");
  private static final Set<String> AI_KEYS =
      Set.of(
          "provider",
          "model",
          "modelVersion",
          "promptSha256",
          "promptNotApplicableReason",
          "corpusVersion",
          "corpusNotApplicableReason");
  private static final Set<String> MEASUREMENT_KEYS =
      Set.of("warmupSeconds", "durationSeconds", "concurrency", "requestMix");
  private static final Set<String> ARTIFACT_KEYS = Set.of("path", "sha256", "summaryRef");

  private final Path repositoryRoot;
  private final ObjectMapper yamlMapper;

  public EvidenceManifestValidator(Path repositoryRoot) {
    this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
  }

  public void check(String targetProject) throws IOException {
    List<String> violations = new ArrayList<>();
    Path manifest = resolveManifest(targetProject, violations);
    if (manifest == null || !Files.isRegularFile(manifest)) {
      violations.add("missing evidence manifest for " + targetProject);
      throw new EvidenceValidationException(violations);
    }

    String markdown = normalize(Files.readString(manifest, StandardCharsets.UTF_8));
    FrontMatter frontMatter = parseFrontMatter(markdown, violations);
    if (frontMatter == null) {
      throw new EvidenceValidationException(violations);
    }

    JsonNode root;
    try {
      root = yamlMapper.readTree(frontMatter.yaml());
    } catch (IOException exception) {
      violations.add("invalid YAML front matter: " + exception.getMessage());
      throw new EvidenceValidationException(violations);
    }

    if (root == null || !root.isObject()) {
      violations.add("front matter must be a YAML object");
      throw new EvidenceValidationException(violations);
    }

    validateExactKeys(root, TOP_LEVEL_KEYS, "manifest", violations);
    validateHeadings(frontMatter.body(), violations);
    validateSchema(root, targetProject, manifest, violations);
    validateGitBoundary(root, targetProject, violations);
    if ("final-audit".equals(targetProject)) {
      validateFinalAuditArtifacts(root.path("artifacts"), manifest.getParent(), violations);
    }

    if (!violations.isEmpty()) {
      throw new EvidenceValidationException(violations);
    }
  }

  private Path resolveManifest(String targetProject, List<String> violations) {
    if ("final-audit".equals(targetProject)) {
      return repositoryRoot.resolve("reviews/evidence/MANIFEST.md");
    }
    if (!PROJECT_ID.matcher(targetProject).matches()) {
      violations.add("invalid targetProject: " + targetProject);
      return null;
    }
    Path projects = repositoryRoot.resolve("projects").normalize();
    Path project = projects.resolve(targetProject).normalize();
    if (!project.getParent().equals(projects) || !Files.isDirectory(project)) {
      violations.add("unknown project directory: projects/" + targetProject);
      return null;
    }
    return project.resolve("evidence/MANIFEST.md");
  }

  private static FrontMatter parseFrontMatter(String markdown, List<String> violations) {
    if (!markdown.startsWith("---\n")) {
      violations.add("MANIFEST.md must start with YAML front matter");
      return null;
    }
    int end = markdown.indexOf("\n---\n", 4);
    if (end < 0) {
      violations.add("YAML front matter closing delimiter is missing");
      return null;
    }
    return new FrontMatter(markdown.substring(4, end), markdown.substring(end + 5));
  }

  private void validateSchema(
      JsonNode root, String targetProject, Path manifest, List<String> violations)
      throws IOException {
    requireInteger(root, "schemaVersion", "manifest", 1, false, violations);
    JsonNode schemaVersion = root.get("schemaVersion");
    if (schemaVersion != null
        && schemaVersion.isIntegralNumber()
        && schemaVersion.intValue() != 1) {
      violations.add("unsupported manifest.schemaVersion: " + schemaVersion.intValue());
    }
    String projectId = requireString(root, "projectId", "manifest", violations);
    if (projectId != null && !targetProject.equals(projectId)) {
      violations.add("projectId does not match targetProject: " + projectId);
    }
    requirePattern(root, "sourceSha", "manifest", COMMIT_SHA, violations);
    validateGeneratedAt(root, violations);
    validateStringArray(root, "intendedTags", "manifest", true, true, violations);
    validateEnvironment(root.path("environment"), violations);
    validateCommands(root.path("commands"), violations);
    validateOptionalSection(
        root, "dataset", "datasetNotApplicableReason", DATASET_KEYS, violations);
    if (!root.path("dataset").isNull() && !root.path("dataset").isMissingNode()) {
      validateDataset(root.path("dataset"), violations);
    }
    validateAiContext(root, targetProject, violations);
    validateOptionalSection(
        root, "measurement", "measurementNotApplicableReason", MEASUREMENT_KEYS, violations);
    if (!root.path("measurement").isNull() && !root.path("measurement").isMissingNode()) {
      validateMeasurement(root.path("measurement"), violations);
    }
    validateArtifacts(root.path("artifacts"), manifest, targetProject, violations);
    validateStringArray(root, "limitations", "manifest", true, false, violations);
  }

  private void validateEnvironment(JsonNode environment, List<String> violations) {
    if (!requireObject(environment, "environment", violations)) {
      return;
    }
    validateExactKeys(environment, ENVIRONMENT_KEYS, "environment", violations);
    requireString(environment, "os", "environment", violations);
    requireString(environment, "cpu", "environment", violations);
    requireInteger(environment, "memoryBytes", "environment", 1, false, violations);
    requireString(environment, "jvm", "environment", violations);
    validateStringArray(environment, "dependencies", "environment", true, false, violations);
  }

  private void validateCommands(JsonNode commands, List<String> violations) {
    if (!requireArray(commands, "commands", true, violations)) {
      return;
    }
    for (int index = 0; index < commands.size(); index++) {
      JsonNode command = commands.get(index);
      String owner = "commands[" + index + "]";
      if (!requireObject(command, owner, violations)) {
        continue;
      }
      validateExactKeys(command, COMMAND_KEYS, owner, violations);
      requireString(command, "command", owner, violations);
      List<String> configPaths =
          validateStringArray(command, "configPaths", owner, true, true, violations);
      for (String configPath : configPaths) {
        resolveExistingPath(repositoryRoot, configPath, owner + ".configPaths", null, violations);
      }
    }
  }

  private void validateOptionalSection(
      JsonNode root,
      String sectionName,
      String reasonName,
      Set<String> keys,
      List<String> violations) {
    JsonNode section = root.get(sectionName);
    JsonNode reason = root.get(reasonName);
    if (section == null || section.isNull()) {
      requireString(root, reasonName, "manifest", violations);
      return;
    }
    if (!requireObject(section, sectionName, violations)) {
      return;
    }
    validateExactKeys(section, keys, sectionName, violations);
    requireExplicitNull(reason, reasonName, violations);
  }

  private void validateDataset(JsonNode dataset, List<String> violations) {
    requireString(dataset, "seed", "dataset", violations);
    requireInteger(dataset, "size", "dataset", 0, false, violations);
    requireString(dataset, "distribution", "dataset", violations);
    requireString(dataset, "version", "dataset", violations);
  }

  private void validateMeasurement(JsonNode measurement, List<String> violations) {
    requireInteger(measurement, "warmupSeconds", "measurement", 0, false, violations);
    requireInteger(measurement, "durationSeconds", "measurement", 1, false, violations);
    requireInteger(measurement, "concurrency", "measurement", 1, false, violations);
    requireString(measurement, "requestMix", "measurement", violations);
  }

  private void validateAiContext(JsonNode root, String targetProject, List<String> violations) {
    boolean aiRequired =
        "final-audit".equals(targetProject)
            || targetProject.startsWith("10-")
            || targetProject.startsWith("11-")
            || targetProject.startsWith("12-");
    JsonNode context = root.get("aiContext");
    JsonNode reason = root.get("aiNotApplicableReason");
    if (!aiRequired) {
      requireExplicitNull(context, "aiContext", violations);
      requireString(root, "aiNotApplicableReason", "manifest", violations);
      return;
    }
    if (!requireObject(context, "aiContext", violations)) {
      return;
    }
    requireExplicitNull(reason, "aiNotApplicableReason", violations);
    validateExactKeys(context, AI_KEYS, "aiContext", violations);
    requireString(context, "provider", "aiContext", violations);
    requireString(context, "model", "aiContext", violations);
    requireString(context, "modelVersion", "aiContext", violations);
    validateValueOrReason(
        context, "promptSha256", "promptNotApplicableReason", CONTENT_SHA, violations);
    validateValueOrReason(context, "corpusVersion", "corpusNotApplicableReason", null, violations);
  }

  private static void validateValueOrReason(
      JsonNode owner,
      String valueName,
      String reasonName,
      Pattern valuePattern,
      List<String> violations) {
    JsonNode value = owner.get(valueName);
    JsonNode reason = owner.get(reasonName);
    if (value == null || value.isNull()) {
      if (reason == null || !reason.isTextual() || reason.textValue().isBlank()) {
        violations.add(reasonName + " must explain why " + valueName + " is null");
      }
      return;
    }
    if (!value.isTextual() || value.textValue().isBlank()) {
      violations.add(valueName + " must be a non-empty string or null");
    } else if (valuePattern != null && !valuePattern.matcher(value.textValue()).matches()) {
      violations.add(valueName + " has an invalid format");
    }
    requireExplicitNull(reason, reasonName, violations);
  }

  private void validateArtifacts(
      JsonNode artifacts, Path manifest, String targetProject, List<String> violations)
      throws IOException {
    if (!requireArray(artifacts, "artifacts", true, violations)) {
      return;
    }
    Path evidenceDirectory = manifest.getParent().toAbsolutePath().normalize();
    for (int index = 0; index < artifacts.size(); index++) {
      JsonNode artifact = artifacts.get(index);
      String owner = "artifacts[" + index + "]";
      if (!requireObject(artifact, owner, violations)) {
        continue;
      }
      validateExactKeys(artifact, ARTIFACT_KEYS, owner, violations);
      String artifactPath = requireString(artifact, "path", owner, violations);
      String expectedSha = requirePattern(artifact, "sha256", owner, CONTENT_SHA, violations);
      String summaryRef = requireString(artifact, "summaryRef", owner, violations);
      Path requiredBase = "final-audit".equals(targetProject) ? null : evidenceDirectory;
      Path resolved =
          resolveExistingPath(
              evidenceDirectory, artifactPath, owner + ".path", requiredBase, violations);
      if (resolved != null && expectedSha != null) {
        String actualSha = sha256(resolved);
        if (!expectedSha.equals(actualSha)) {
          violations.add(owner + " checksum mismatch: " + artifactPath);
        }
      }
      validateSummaryReference(evidenceDirectory, summaryRef, owner, violations);
    }
  }

  private void validateSummaryReference(
      Path evidenceDirectory, String summaryRef, String owner, List<String> violations)
      throws IOException {
    if (summaryRef == null) {
      return;
    }
    String[] parts = summaryRef.split("#", 2);
    Path summary =
        resolveExistingPath(evidenceDirectory, parts[0], owner + ".summaryRef", null, violations);
    if (summary == null || parts.length < 2 || parts[1].isBlank()) {
      violations.add(owner + ".summaryRef must include an existing heading anchor");
      return;
    }
    Set<String> anchors = new HashSet<>();
    for (String line : normalize(Files.readString(summary, StandardCharsets.UTF_8)).split("\n")) {
      if (line.matches("^#{1,6}\\s+.+$")) {
        anchors.add(slug(line.replaceFirst("^#{1,6}\\s+", "")));
      }
    }
    if (!anchors.contains(parts[1].toLowerCase())) {
      violations.add(owner + ".summaryRef has no matching heading: " + summaryRef);
    }
  }

  private void validateGitBoundary(JsonNode root, String targetProject, List<String> violations) {
    String sourceSha = textValue(root.get("sourceSha"));
    if (sourceSha == null || !COMMIT_SHA.matcher(sourceSha).matches()) {
      return;
    }
    GitResult status = git("status", "--porcelain", "--untracked-files=all");
    if (status.exitCode() != 0) {
      violations.add("unable to inspect Git status: " + status.output());
      return;
    }
    if (!status.output().isBlank()) {
      violations.add("evidenceCheck requires a clean candidate worktree");
    }
    GitResult source = git("cat-file", "-e", sourceSha + "^{commit}");
    if (source.exitCode() != 0) {
      violations.add("sourceSha is not a local commit: " + sourceSha);
      return;
    }
    GitResult ancestor = git("merge-base", "--is-ancestor", sourceSha, "HEAD");
    if (ancestor.exitCode() != 0) {
      violations.add("sourceSha is not an ancestor of candidate HEAD: " + sourceSha);
      return;
    }
    GitResult changed = git("diff", "--name-only", sourceSha + "..HEAD");
    if (changed.exitCode() != 0) {
      violations.add("unable to inspect sourceSha..HEAD: " + changed.output());
    } else {
      changed
          .output()
          .lines()
          .filter(path -> !path.isBlank())
          .forEach(
              path -> {
                if (!isAllowedEvidenceChange(path, targetProject)) {
                  violations.add("change outside evidence allowlist: " + path);
                }
              });
    }
    for (String tag : stringValues(root.path("intendedTags"))) {
      if (git("show-ref", "--verify", "--quiet", "refs/tags/" + tag).exitCode() == 0) {
        violations.add("intended tag must not exist before evidence validation: " + tag);
      }
    }
  }

  private static boolean isAllowedEvidenceChange(String path, String targetProject) {
    if ("README.md".equals(path) || path.startsWith("reviews/")) {
      return true;
    }
    if (path.startsWith("projects/") && path.contains("/evidence/")) {
      return true;
    }
    if ("final-audit".equals(targetProject)) {
      return false;
    }
    String prefix = "projects/" + targetProject + "/";
    return path.equals(prefix + "RESULTS.md")
        || path.equals(prefix + "PORTFOLIO_REVIEW.md")
        || path.equals(prefix + "REDTEAM_REVIEW.md");
  }

  private void validateFinalAuditArtifacts(
      JsonNode artifacts, Path evidenceDirectory, List<String> violations) throws IOException {
    if (!artifacts.isArray()) {
      return;
    }
    Set<Path> artifactPaths = new HashSet<>();
    for (JsonNode artifact : artifacts) {
      String path = textValue(artifact.get("path"));
      if (path != null) {
        Path resolved = evidenceDirectory.resolve(path).normalize().toAbsolutePath();
        if (resolved.startsWith(repositoryRoot)) {
          artifactPaths.add(resolved);
        }
      }
    }
    Path projects = repositoryRoot.resolve("projects");
    List<Path> manifests;
    try (Stream<Path> directories = Files.list(projects)) {
      manifests =
          directories
              .filter(Files::isDirectory)
              .filter(path -> PROJECT_ID.matcher(path.getFileName().toString()).matches())
              .map(path -> path.resolve("evidence/MANIFEST.md").toAbsolutePath().normalize())
              .sorted()
              .toList();
    }
    if (manifests.size() != 12) {
      violations.add(
          "final-audit requires exactly 12 project directories, found " + manifests.size());
    }
    for (Path projectManifest : manifests) {
      if (!artifactPaths.contains(projectManifest)) {
        violations.add(
            "final-audit artifact is missing project manifest: "
                + repositoryRoot.relativize(projectManifest));
      }
    }
  }

  private static void validateGeneratedAt(JsonNode root, List<String> violations) {
    String generatedAt = requireString(root, "generatedAt", "manifest", violations);
    if (generatedAt == null) {
      return;
    }
    if (!generatedAt.endsWith("Z")) {
      violations.add("generatedAt must be UTC and end with Z");
      return;
    }
    try {
      Instant.parse(generatedAt);
    } catch (DateTimeParseException exception) {
      violations.add("generatedAt must be ISO-8601: " + generatedAt);
    }
  }

  private static void validateHeadings(String body, List<String> violations) {
    for (String required : REQUIRED_HEADINGS) {
      long count = body.lines().filter(line -> line.equals("## " + required)).count();
      if (count != 1) {
        violations.add(
            "required H2 must appear exactly once: " + required + " (found " + count + ")");
      }
    }
  }

  private static void validateExactKeys(
      JsonNode object, Set<String> expected, String owner, List<String> violations) {
    if (!object.isObject()) {
      return;
    }
    Set<String> actual = new LinkedHashSet<>();
    object.fieldNames().forEachRemaining(actual::add);
    Set<String> missing = new LinkedHashSet<>(expected);
    missing.removeAll(actual);
    Set<String> unknown = new LinkedHashSet<>(actual);
    unknown.removeAll(expected);
    if (!missing.isEmpty()) {
      violations.add(owner + " is missing keys: " + missing);
    }
    if (!unknown.isEmpty()) {
      violations.add(owner + " has unknown keys: " + unknown);
    }
  }

  private static boolean requireObject(JsonNode node, String owner, List<String> violations) {
    if (node == null || !node.isObject()) {
      violations.add(owner + " must be an object");
      return false;
    }
    return true;
  }

  private static boolean requireArray(
      JsonNode node, String owner, boolean nonEmpty, List<String> violations) {
    if (node == null || !node.isArray()) {
      violations.add(owner + " must be a list");
      return false;
    }
    if (nonEmpty && node.isEmpty()) {
      violations.add(owner + " must not be empty");
      return false;
    }
    return true;
  }

  private static String requireString(
      JsonNode owner, String field, String ownerName, List<String> violations) {
    JsonNode value = owner == null ? null : owner.get(field);
    if (value == null || !value.isTextual() || value.textValue().isBlank()) {
      violations.add(ownerName + "." + field + " must be a non-empty string");
      return null;
    }
    return value.textValue();
  }

  private static String requirePattern(
      JsonNode owner, String field, String ownerName, Pattern pattern, List<String> violations) {
    String value = requireString(owner, field, ownerName, violations);
    if (value != null && !pattern.matcher(value).matches()) {
      violations.add(ownerName + "." + field + " has an invalid format");
      return null;
    }
    return value;
  }

  private static void requireInteger(
      JsonNode owner,
      String field,
      String ownerName,
      long minimum,
      boolean allowNull,
      List<String> violations) {
    JsonNode value = owner == null ? null : owner.get(field);
    if (allowNull && (value == null || value.isNull())) {
      return;
    }
    if (value == null || !value.isIntegralNumber() || value.longValue() < minimum) {
      violations.add(
          ownerName + "." + field + " must be an integer greater than or equal to " + minimum);
    }
  }

  private static List<String> validateStringArray(
      JsonNode owner,
      String field,
      String ownerName,
      boolean nonEmpty,
      boolean unique,
      List<String> violations) {
    JsonNode values = owner == null ? null : owner.get(field);
    if (!requireArray(values, ownerName + "." + field, nonEmpty, violations)) {
      return List.of();
    }
    List<String> output = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (int index = 0; index < values.size(); index++) {
      JsonNode value = values.get(index);
      if (!value.isTextual() || value.textValue().isBlank()) {
        violations.add(ownerName + "." + field + "[" + index + "] must be a non-empty string");
      } else {
        output.add(value.textValue());
        if (unique && !seen.add(value.textValue())) {
          violations.add(ownerName + "." + field + " contains a duplicate: " + value.textValue());
        }
      }
    }
    return output;
  }

  private static void requireExplicitNull(JsonNode value, String field, List<String> violations) {
    if (value == null || !value.isNull()) {
      violations.add(field + " must be explicitly null");
    }
  }

  private Path resolveExistingPath(
      Path base, String rawPath, String owner, Path requiredBase, List<String> violations) {
    if (rawPath == null) {
      return null;
    }
    Path path = Path.of(rawPath);
    if (path.isAbsolute()) {
      violations.add(owner + " must be repository-relative: " + rawPath);
      return null;
    }
    Path resolved = base.resolve(path).normalize().toAbsolutePath();
    if (!resolved.startsWith(repositoryRoot)) {
      violations.add(owner + " escapes repository root: " + rawPath);
      return null;
    }
    if (requiredBase != null && !resolved.startsWith(requiredBase)) {
      violations.add(owner + " escapes project evidence directory: " + rawPath);
      return null;
    }
    if (!Files.isRegularFile(resolved)) {
      violations.add(owner + " does not exist: " + rawPath);
      return null;
    }
    try {
      Path realRepositoryRoot = repositoryRoot.toRealPath();
      Path realResolved = resolved.toRealPath();
      if (!realResolved.startsWith(realRepositoryRoot)) {
        violations.add(owner + " resolves outside repository root: " + rawPath);
        return null;
      }
      if (requiredBase != null && !realResolved.startsWith(requiredBase.toRealPath())) {
        violations.add(owner + " resolves outside project evidence directory: " + rawPath);
        return null;
      }
      return realResolved;
    } catch (IOException exception) {
      violations.add(owner + " cannot be resolved safely: " + rawPath);
      return null;
    }
  }

  private GitResult git(String... arguments) {
    List<String> command = new ArrayList<>();
    command.add("git");
    command.addAll(List.of(arguments));
    try {
      Process process =
          new ProcessBuilder(command)
              .directory(repositoryRoot.toFile())
              .redirectErrorStream(true)
              .start();
      String output =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      return new GitResult(process.waitFor(), output);
    } catch (IOException exception) {
      return new GitResult(127, exception.getMessage());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return new GitResult(130, "Git command interrupted");
    }
  }

  private static List<String> stringValues(JsonNode array) {
    if (!array.isArray()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    for (JsonNode value : array) {
      if (value.isTextual()) {
        values.add(value.textValue());
      }
    }
    return values;
  }

  private static String textValue(JsonNode node) {
    return node != null && node.isTextual() ? node.textValue() : null;
  }

  private static String sha256(Path file) throws IOException {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private static String normalize(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n');
  }

  private static String slug(String heading) {
    return heading
        .strip()
        .toLowerCase()
        .replaceAll("[^\\p{L}\\p{N} _-]", "")
        .replace(' ', '-')
        .replaceAll("-+", "-");
  }

  private record FrontMatter(String yaml, String body) {}

  private record GitResult(int exitCode, String output) {}

  public static final class EvidenceValidationException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> violations;

    EvidenceValidationException(List<String> violations) {
      super("Evidence validation failed:\n - " + String.join("\n - ", violations));
      this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
      return violations;
    }
  }
}

package dev.whoisyourbias.financial.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KnowledgeHarness {

  private static final int TARGET_CHUNK_SIZE = 1_500;
  private static final int CHUNK_OVERLAP = 200;
  private static final Set<String> ALLOWED_KINDS =
      Set.of("policy", "architecture", "domain", "plan", "exit-criteria", "review", "external");
  private static final Set<String> ALLOWED_STATUSES =
      Set.of("planned", "verified", "unknown", "contradicted");
  private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.+?)\\s*$");
  private static final Pattern ANY_HEADING = Pattern.compile("^#{1,6}\\s+(.+?)\\s*$");
  private static final Pattern LINK =
      Pattern.compile("(?<!!)\\[[^\\]]*]\\((?<target><[^>]+>|[^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
  private static final Pattern TABLE_SEPARATOR =
      Pattern.compile("^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$");

  private final Path repositoryRoot;
  private final ObjectMapper yamlMapper;
  private final ObjectMapper jsonMapper;

  public KnowledgeHarness(Path repositoryRoot) {
    this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
    this.jsonMapper = new ObjectMapper();
    this.jsonMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  }

  public void check() throws IOException {
    LoadedCatalog loaded = loadCatalog();
    List<String> violations = validateCatalog(loaded);
    if (violations.isEmpty()) {
      List<Chunk> first = chunks(loaded.catalog().documents());
      List<Chunk> second = chunks(loaded.catalog().documents());
      if (!serialize(first).equals(serialize(second))) {
        violations.add("same input produced different chunk output");
      }
      Set<String> chunkIds = new HashSet<>();
      for (Chunk chunk : first) {
        if (!chunkIds.add(chunk.id())) {
          violations.add("duplicate chunk ID: " + chunk.id());
        }
      }
    }
    if (!violations.isEmpty()) {
      throw new KnowledgeValidationException(violations);
    }
  }

  public Path export() throws IOException {
    check();
    LoadedCatalog loaded = loadCatalog();
    List<String> lines = serialize(chunks(loaded.catalog().documents()));
    Path output = repositoryRoot.resolve("build/knowledge/knowledge.jsonl");
    Files.createDirectories(output.getParent());
    Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
    Files.writeString(temporary, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    try {
      Files.move(
          temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException atomicMoveUnsupported) {
      Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
    }
    return output;
  }

  private LoadedCatalog loadCatalog() throws IOException {
    Path catalogPath = repositoryRoot.resolve("docs/domain/catalog.yaml");
    Path sourcesPath = repositoryRoot.resolve("docs/domain/sources.yaml");
    if (!Files.isRegularFile(catalogPath) || !Files.isRegularFile(sourcesPath)) {
      throw new IOException("docs/domain/catalog.yaml and sources.yaml are required");
    }
    Catalog catalog = yamlMapper.readValue(catalogPath.toFile(), Catalog.class);
    SourceCatalog sources = yamlMapper.readValue(sourcesPath.toFile(), SourceCatalog.class);
    return new LoadedCatalog(catalog, sources);
  }

  private List<String> validateCatalog(LoadedCatalog loaded) throws IOException {
    List<String> violations = new ArrayList<>();
    List<DocumentSpec> documents =
        loaded.catalog().documents() == null ? List.of() : loaded.catalog().documents();
    List<SourceSpec> sources =
        loaded.sources().sources() == null ? List.of() : loaded.sources().sources();
    Set<String> documentIds = new LinkedHashSet<>();
    Set<String> sourceIds = new LinkedHashSet<>();

    for (SourceSpec source : sources) {
      if (isBlank(source.id()) || !sourceIds.add(source.id())) {
        violations.add("blank or duplicate source ID: " + source.id());
      }
      if (isBlank(source.url()) || !source.url().startsWith("https://")) {
        violations.add("source must use https: " + source.id());
      }
      LocalDate checkedAt = parseDate(source.checkedAt(), "source " + source.id(), violations);
      if (checkedAt != null && checkedAt.isAfter(LocalDate.now())) {
        violations.add("checkedAt is in the future for source: " + source.id());
      }
    }

    for (DocumentSpec document : documents) {
      if (isBlank(document.id()) || !documentIds.add(document.id())) {
        violations.add("blank or duplicate document ID: " + document.id());
      }
      if (!ALLOWED_KINDS.contains(document.kind())) {
        violations.add("unsupported kind for " + document.id() + ": " + document.kind());
      }
      if (!ALLOWED_STATUSES.contains(document.status())) {
        violations.add("unsupported status for " + document.id() + ": " + document.status());
      }
      parseDate(document.reviewedAt(), "document " + document.id(), violations);
      Path documentPath = resolveRepositoryPath(document.path(), violations, document.id());
      if (documentPath == null || !Files.isRegularFile(documentPath)) {
        violations.add("missing catalog path for " + document.id() + ": " + document.path());
        continue;
      }
      List<String> sourceRefs = nullToEmpty(document.sourceRefs());
      List<String> evidenceRefs = nullToEmpty(document.evidenceRefs());
      for (String sourceRef : sourceRefs) {
        if (!sourceIds.contains(sourceRef)) {
          violations.add("unknown sourceRef for " + document.id() + ": " + sourceRef);
        }
      }
      for (String evidenceRef : evidenceRefs) {
        String evidencePath = evidenceRef.split("#", 2)[0];
        Path resolved = resolveRepositoryPath(evidencePath, violations, document.id());
        if (resolved == null || !Files.exists(resolved)) {
          violations.add("unknown evidenceRef for " + document.id() + ": " + evidenceRef);
        }
      }
      if ("verified".equals(document.status()) && sourceRefs.isEmpty() && evidenceRefs.isEmpty()) {
        violations.add("verified document has no sourceRefs or evidenceRefs: " + document.id());
      }
      validateLinks(documentPath, violations);
    }

    if (documents.isEmpty()) {
      violations.add("catalog contains no documents");
    }
    return violations;
  }

  private static LocalDate parseDate(String value, String owner, List<String> violations) {
    if (isBlank(value)) {
      violations.add("missing date for " + owner);
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException exception) {
      violations.add("invalid date for " + owner + ": " + value);
      return null;
    }
  }

  private Path resolveRepositoryPath(String rawPath, List<String> violations, String owner) {
    if (isBlank(rawPath)) {
      violations.add("blank repository path for " + owner);
      return null;
    }
    Path candidate = Path.of(rawPath);
    if (candidate.isAbsolute()) {
      violations.add("absolute repository path for " + owner + ": " + rawPath);
      return null;
    }
    Path resolved = repositoryRoot.resolve(candidate).normalize();
    if (!resolved.startsWith(repositoryRoot)) {
      violations.add("repository path escapes root for " + owner + ": " + rawPath);
      return null;
    }
    return resolved;
  }

  private void validateLinks(Path document, List<String> violations) throws IOException {
    String markdown = normalize(Files.readString(document, StandardCharsets.UTF_8));
    Matcher matcher = LINK.matcher(markdown);
    while (matcher.find()) {
      String rawTarget = matcher.group("target");
      if (rawTarget.startsWith("<") && rawTarget.endsWith(">")) {
        rawTarget = rawTarget.substring(1, rawTarget.length() - 1);
      }
      if (rawTarget.startsWith("http://")
          || rawTarget.startsWith("https://")
          || rawTarget.startsWith("mailto:")) {
        continue;
      }
      String[] targetParts = rawTarget.split("#", 2);
      String decodedPath = URLDecoder.decode(targetParts[0], StandardCharsets.UTF_8);
      Path targetDocument =
          decodedPath.isBlank() ? document : document.getParent().resolve(decodedPath).normalize();
      if (!targetDocument.startsWith(repositoryRoot) || !Files.isRegularFile(targetDocument)) {
        violations.add("broken internal link in " + relative(document) + ": " + rawTarget);
        continue;
      }
      if (targetParts.length == 2 && !targetParts[1].isBlank()) {
        String anchor = targetParts[1].toLowerCase(Locale.ROOT);
        if (!anchors(targetDocument).contains(anchor)) {
          violations.add("broken heading anchor in " + relative(document) + ": " + rawTarget);
        }
      }
    }
  }

  private Set<String> anchors(Path document) throws IOException {
    Set<String> anchors = new LinkedHashSet<>();
    Map<String, Integer> occurrences = new HashMap<>();
    for (String line :
        normalize(Files.readString(document, StandardCharsets.UTF_8)).split("\n", -1)) {
      Matcher matcher = ANY_HEADING.matcher(line);
      if (!matcher.matches()) {
        continue;
      }
      String base = slug(matcher.group(1));
      int occurrence = occurrences.merge(base, 1, Integer::sum);
      anchors.add(occurrence == 1 ? base : base + "-" + (occurrence - 1));
    }
    return anchors;
  }

  private List<Chunk> chunks(List<DocumentSpec> documents) throws IOException {
    List<Chunk> output = new ArrayList<>();
    for (DocumentSpec document :
        documents.stream().sorted((left, right) -> left.id().compareTo(right.id())).toList()) {
      Path source = repositoryRoot.resolve(document.path()).normalize();
      String markdown = normalize(Files.readString(source, StandardCharsets.UTF_8));
      List<Section> sections = sections(markdown);
      for (Section section : sections) {
        List<BodyChunk> bodies = chunkSection(section);
        for (int index = 0; index < bodies.size(); index++) {
          BodyChunk body = bodies.get(index);
          String headingSlug =
              section.headingPath().isEmpty()
                  ? "root"
                  : slug(section.headingPath().get(section.headingPath().size() - 1));
          String id =
              document.id()
                  + "-"
                  + headingSlug
                  + "-"
                  + section.headingOccurrence()
                  + "-"
                  + (index + 1);
          output.add(
              new Chunk(
                  id,
                  document.id(),
                  document.title(),
                  section.headingPath(),
                  document.path().replace('\\', '/'),
                  body.startLine(),
                  body.endLine(),
                  document.status(),
                  nullToEmpty(document.domains()),
                  document.version(),
                  nullToEmpty(document.sourceRefs()),
                  nullToEmpty(document.evidenceRefs()),
                  body.oversized(),
                  body.text(),
                  sha256(body.text())));
        }
      }
    }
    return output;
  }

  private List<Section> sections(String markdown) {
    String[] lines = markdown.split("\n", -1);
    List<Section> output = new ArrayList<>();
    List<String> headingPath = new ArrayList<>();
    Map<String, Integer> headingOccurrences = new HashMap<>();
    List<String> body = new ArrayList<>();
    int sectionStart = 1;
    int currentOccurrence = 1;
    boolean inFence = false;
    String fenceMarker = null;

    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      String trimmed = line.stripLeading();
      if (isFence(trimmed)) {
        String marker = trimmed.substring(0, 3);
        if (!inFence) {
          inFence = true;
          fenceMarker = marker;
        } else if (Objects.equals(marker, fenceMarker)) {
          inFence = false;
          fenceMarker = null;
        }
      }
      Matcher heading = HEADING.matcher(line);
      if (!inFence && heading.matches()) {
        addSection(output, headingPath, currentOccurrence, body, sectionStart, index);
        int level = heading.group(1).length();
        while (headingPath.size() >= level) {
          headingPath.remove(headingPath.size() - 1);
        }
        String title = heading.group(2).trim();
        headingPath.add(title);
        String occurrenceKey = String.join(" / ", headingPath);
        currentOccurrence = headingOccurrences.merge(occurrenceKey, 1, Integer::sum);
        body = new ArrayList<>();
        sectionStart = index + 2;
      } else {
        body.add(line);
      }
    }
    addSection(output, headingPath, currentOccurrence, body, sectionStart, lines.length);
    return output;
  }

  private static void addSection(
      List<Section> sections,
      List<String> headingPath,
      int occurrence,
      List<String> body,
      int startLine,
      int endLine) {
    while (!body.isEmpty() && body.get(0).isBlank()) {
      body.remove(0);
      startLine++;
    }
    while (!body.isEmpty() && body.get(body.size() - 1).isBlank()) {
      body.remove(body.size() - 1);
      endLine--;
    }
    if (!body.isEmpty()) {
      sections.add(
          new Section(
              List.copyOf(headingPath),
              occurrence,
              String.join("\n", body),
              startLine,
              Math.max(startLine, endLine)));
    }
  }

  private List<BodyChunk> chunkSection(Section section) {
    List<Block> blocks = blocks(section);
    List<BodyChunk> output = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int currentStart = section.startLine();
    int currentEnd = section.startLine();
    boolean currentAtomic = false;

    for (Block block : blocks) {
      if (block.atomic() && block.text().length() > TARGET_CHUNK_SIZE) {
        flush(output, current, currentStart, currentEnd, false);
        output.add(new BodyChunk(block.text(), block.startLine(), block.endLine(), true));
        currentStart = block.endLine() + 1;
        currentEnd = currentStart;
        currentAtomic = true;
        continue;
      }
      List<Block> pieces = block.atomic() ? List.of(block) : splitLargeBlock(block);
      for (Block piece : pieces) {
        int separator = current.isEmpty() ? 0 : 2;
        if (!current.isEmpty()
            && current.length() + separator + piece.text().length() > TARGET_CHUNK_SIZE) {
          String overlap = currentAtomic ? "" : suffix(current.toString(), CHUNK_OVERLAP);
          int overlapStart = currentStart;
          flush(output, current, currentStart, currentEnd, false);
          if (!overlap.isBlank()) {
            current.append(overlap);
            currentStart = overlapStart;
            currentEnd = piece.startLine();
          }
        }
        if (!current.isEmpty()) {
          current.append("\n\n");
        } else {
          currentStart = piece.startLine();
        }
        current.append(piece.text());
        currentEnd = piece.endLine();
        currentAtomic = piece.atomic();
      }
    }
    flush(output, current, currentStart, currentEnd, false);
    return output;
  }

  private List<Block> blocks(Section section) {
    String[] lines = section.text().split("\n", -1);
    List<Block> output = new ArrayList<>();
    int index = 0;
    while (index < lines.length) {
      while (index < lines.length && lines[index].isBlank()) {
        index++;
      }
      if (index >= lines.length) {
        break;
      }
      int start = index;
      String trimmed = lines[index].stripLeading();
      if (isFence(trimmed)) {
        String marker = trimmed.substring(0, 3);
        index++;
        while (index < lines.length) {
          if (lines[index].stripLeading().startsWith(marker)) {
            index++;
            break;
          }
          index++;
        }
        output.add(
            new Block(
                join(lines, start, index),
                section.startLine() + start,
                section.startLine() + index - 1,
                true));
        continue;
      }
      while (index < lines.length && !lines[index].isBlank()) {
        index++;
      }
      String text = join(lines, start, index);
      String[] candidate = text.split("\n");
      boolean table =
          candidate.length >= 2
              && candidate[0].contains("|")
              && TABLE_SEPARATOR.matcher(candidate[1]).matches();
      output.add(
          new Block(text, section.startLine() + start, section.startLine() + index - 1, table));
    }
    return output;
  }

  private List<Block> splitLargeBlock(Block block) {
    if (block.text().length() <= TARGET_CHUNK_SIZE) {
      return List.of(block);
    }
    List<Block> pieces = new ArrayList<>();
    int start = 0;
    while (start < block.text().length()) {
      int desiredEnd = Math.min(start + TARGET_CHUNK_SIZE, block.text().length());
      int end = desiredEnd;
      if (desiredEnd < block.text().length()) {
        int whitespace = block.text().lastIndexOf(' ', desiredEnd);
        if (whitespace > start + TARGET_CHUNK_SIZE / 2) {
          end = whitespace;
        }
      }
      String piece = block.text().substring(start, end).strip();
      pieces.add(new Block(piece, block.startLine(), block.endLine(), true));
      if (end >= block.text().length()) {
        break;
      }
      start = Math.max(end - CHUNK_OVERLAP, start + 1);
    }
    return pieces;
  }

  private static void flush(
      List<BodyChunk> output,
      StringBuilder current,
      int startLine,
      int endLine,
      boolean oversized) {
    if (current.isEmpty()) {
      return;
    }
    String text = current.toString().strip();
    if (!text.isEmpty()) {
      output.add(new BodyChunk(text, startLine, endLine, oversized));
    }
    current.setLength(0);
  }

  private List<String> serialize(List<Chunk> chunks) throws JsonProcessingException {
    List<String> lines = new ArrayList<>(chunks.size());
    for (Chunk chunk : chunks) {
      Map<String, Object> value = new LinkedHashMap<>();
      value.put("id", chunk.id());
      value.put("documentId", chunk.documentId());
      value.put("title", chunk.title());
      value.put("headingPath", chunk.headingPath());
      value.put("sourcePath", chunk.sourcePath());
      value.put("sourceStartLine", chunk.sourceStartLine());
      value.put("sourceEndLine", chunk.sourceEndLine());
      value.put("status", chunk.status());
      value.put("domains", chunk.domains());
      value.put("version", chunk.version());
      value.put("sourceRefs", chunk.sourceRefs());
      value.put("evidenceRefs", chunk.evidenceRefs());
      value.put("oversized", chunk.oversized());
      value.put("body", chunk.body());
      value.put("checksum", chunk.checksum());
      lines.add(jsonMapper.writeValueAsString(value));
    }
    return lines;
  }

  static String normalize(String input) {
    return Normalizer.normalize(input, Normalizer.Form.NFC)
        .replace("\r\n", "\n")
        .replace('\r', '\n');
  }

  static String slug(String input) {
    String normalized = normalize(input).toLowerCase(Locale.ROOT);
    StringBuilder result = new StringBuilder();
    boolean separator = false;
    for (int index = 0; index < normalized.length(); ) {
      int codePoint = normalized.codePointAt(index);
      index += Character.charCount(codePoint);
      if (Character.isLetterOrDigit(codePoint) || codePoint == '_') {
        if (separator && !result.isEmpty() && result.charAt(result.length() - 1) != '-') {
          result.append('-');
        }
        result.appendCodePoint(codePoint);
        separator = false;
      } else if (codePoint == '-' || Character.isWhitespace(codePoint)) {
        separator = true;
      }
    }
    return result.isEmpty() ? "section" : result.toString();
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder output = new StringBuilder(digest.length * 2);
      for (byte item : digest) {
        output.append(String.format("%02x", item));
      }
      return output.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  private static boolean isFence(String line) {
    return line.startsWith("```") || line.startsWith("~~~");
  }

  private static String suffix(String value, int length) {
    if (value.length() <= length) {
      return value;
    }
    int start = value.length() - length;
    while (start < value.length() && !Character.isWhitespace(value.charAt(start))) {
      start++;
    }
    return value.substring(Math.min(start, value.length())).strip();
  }

  private static String join(String[] lines, int start, int endExclusive) {
    StringBuilder result = new StringBuilder();
    for (int index = start; index < endExclusive; index++) {
      if (!result.isEmpty()) {
        result.append('\n');
      }
      result.append(lines[index]);
    }
    return result.toString();
  }

  private static <T> List<T> nullToEmpty(List<T> values) {
    return values == null ? List.of() : List.copyOf(values);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String relative(Path path) {
    return repositoryRoot.relativize(path).toString().replace('\\', '/');
  }

  public record Catalog(List<DocumentSpec> documents) {}

  public record SourceCatalog(List<SourceSpec> sources) {}

  public record DocumentSpec(
      String id,
      String title,
      String path,
      String kind,
      List<String> domains,
      String status,
      String version,
      String reviewedAt,
      List<String> appliesTo,
      List<String> sourceRefs,
      List<String> evidenceRefs) {}

  public record SourceSpec(
      String id,
      String title,
      String url,
      String publisher,
      String checkedAt,
      List<String> domains,
      String scope,
      boolean storesOriginal) {}

  private record LoadedCatalog(Catalog catalog, SourceCatalog sources) {}

  private record Section(
      List<String> headingPath, int headingOccurrence, String text, int startLine, int endLine) {}

  private record Block(String text, int startLine, int endLine, boolean atomic) {}

  private record BodyChunk(String text, int startLine, int endLine, boolean oversized) {}

  private record Chunk(
      String id,
      String documentId,
      String title,
      List<String> headingPath,
      String sourcePath,
      int sourceStartLine,
      int sourceEndLine,
      String status,
      List<String> domains,
      String version,
      List<String> sourceRefs,
      List<String> evidenceRefs,
      boolean oversized,
      String body,
      String checksum) {}

  public static final class KnowledgeValidationException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    KnowledgeValidationException(List<String> violations) {
      super("Knowledge validation failed:\n- " + String.join("\n- ", violations));
    }
  }
}

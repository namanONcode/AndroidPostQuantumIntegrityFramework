package com.anchorpq.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

/**
 * Gradle task that generates integrity metadata JSON file.
 *
 * <p>Output format:
 *
 * <pre>{@code
 * {
 *   "version": "1.0.0",
 *   "variant": "debug",
 *   "hashAlgorithm": "SHA-256",
 *   "merkleRoot": "hex-string",
 *   "timestamp": "ISO-8601-timestamp",
 *   "signerFingerprint": "optional-fingerprint"
 * }
 * }</pre>
 */
public abstract class GenerateMetadataTask extends DefaultTask {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;

  /** The variant name (e.g., "debug", "release"). */
  @Input
  public abstract Property<String> getVariantName();

  /** Whether the task is enabled. */
  @Input
  public abstract Property<Boolean> getPluginEnabled();

  /** Hash algorithm used for Merkle tree. */
  @Input
  public abstract Property<String> getAlgorithm();

  /** Application version. */
  @Input
  public abstract Property<String> getVersion();

  /** Optional signer fingerprint. */
  @Input
  @Optional
  public abstract Property<String> getSignerFingerprint();

  /** File containing the Merkle root. */
  @InputFile
  public abstract RegularFileProperty getMerkleRootFile();

  /** Output directory for the metadata file. */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDirectory();

  @TaskAction
  public void generate() {
    if (!getPluginEnabled().getOrElse(true)) {
      getLogger().lifecycle("GenerateMetadataTask is disabled, skipping");
      return;
    }

    getLogger().lifecycle("Generating integrity metadata for variant: {}", getVariantName().get());

    try {
      // Read Merkle root from file
      File merkleFile = getMerkleRootFile().getAsFile().get();
      if (!merkleFile.exists()) {
        throw new GradleException("Merkle root file not found: " + merkleFile.getPath());
      }

      String merkleRoot = Files.readString(merkleFile.toPath()).trim();

      if (merkleRoot.isEmpty()) {
        throw new GradleException("Merkle root file is empty");
      }

      // Build metadata
      Map<String, Object> metadata = buildMetadata(merkleRoot);

      // Write JSON
      writeMetadataJson(metadata);

      // Also write XML if configured
      writeMetadataXml(metadata);

    } catch (IOException e) {
      throw new GradleException("Failed to generate metadata: " + e.getMessage(), e);
    }
  }

  /** Builds the metadata map. */
  private Map<String, Object> buildMetadata(String merkleRoot) {
    Map<String, Object> metadata = new LinkedHashMap<>();

    metadata.put("version", getVersion().getOrElse("1.0.0"));
    metadata.put("variant", getVariantName().get());
    metadata.put("hashAlgorithm", getAlgorithm().getOrElse("SHA-256"));
    metadata.put("merkleRoot", merkleRoot);
    metadata.put("timestamp", Instant.now().atOffset(ZoneOffset.UTC).format(TIMESTAMP_FORMAT));
    metadata.put("leafCount", countLeafFiles());

    String fingerprint = getSignerFingerprint().getOrNull();
    if (fingerprint != null && !fingerprint.isEmpty()) {
      metadata.put("signerFingerprint", fingerprint);
    }

    // Add plugin metadata
    Map<String, String> pluginInfo = new LinkedHashMap<>();
    pluginInfo.put("name", "Anchor PQ Integrity Plugin");
    pluginInfo.put("pluginVersion", "1.0.0");
    metadata.put("plugin", pluginInfo);

    return metadata;
  }

  /** Counts the number of leaf files used in Merkle computation. This is informational only. */
  private int countLeafFiles() {
    try {
      File outputDir = getOutputDirectory().getAsFile().get();
      File leafCountFile = new File(outputDir, "leaf-count.txt");
      if (leafCountFile.exists()) {
        return Integer.parseInt(Files.readString(leafCountFile.toPath()).trim());
      }
    } catch (Exception e) {
      getLogger().debug("Could not read leaf count: " + e.getMessage());
    }
    return -1;
  }

  /** Writes metadata as JSON. */
  private void writeMetadataJson(Map<String, Object> metadata) throws IOException {
    File outputDir = getOutputDirectory().getAsFile().get();
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    File jsonFile = new File(outputDir, "integrity.json");

    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    String json = gson.toJson(metadata);
    Files.writeString(jsonFile.toPath(), json);

    getLogger().lifecycle("Integrity metadata written to: {}", jsonFile.getAbsolutePath());
    getLogger().lifecycle("Metadata content:\n{}", json);
  }

  /** Writes metadata as XML (optional). */
  private void writeMetadataXml(Map<String, Object> metadata) throws IOException {
    File outputDir = getOutputDirectory().getAsFile().get();
    File xmlFile = new File(outputDir, "integrity.xml");

    StringBuilder xml = new StringBuilder();
    xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append("<integrity>\n");

    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, String> nested = (Map<String, String>) value;
        xml.append("  <").append(key).append(">\n");
        for (Map.Entry<String, String> nestedEntry : nested.entrySet()) {
          xml.append("    <")
              .append(nestedEntry.getKey())
              .append(">")
              .append(escapeXml(nestedEntry.getValue()))
              .append("</")
              .append(nestedEntry.getKey())
              .append(">\n");
        }
        xml.append("  </").append(key).append(">\n");
      } else {
        xml.append("  <")
            .append(key)
            .append(">")
            .append(escapeXml(String.valueOf(value)))
            .append("</")
            .append(key)
            .append(">\n");
      }
    }

    xml.append("</integrity>\n");

    Files.writeString(xmlFile.toPath(), xml.toString());
    getLogger().debug("XML metadata written to: {}", xmlFile.getAbsolutePath());
  }

  /** Escapes special XML characters. */
  private String escapeXml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}

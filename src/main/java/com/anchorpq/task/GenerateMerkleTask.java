package com.anchorpq.task;

import com.anchorpq.merkle.HashUtils;
import com.anchorpq.merkle.MerkleTree;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

/**
 * Gradle task that generates a Merkle root from compiled bytecode.
 *
 * <p>This task: 1. Walks the classes directory 2. Filters out generated classes (R.class,
 * BuildConfig.class, etc.) 3. Sorts files lexicographically for determinism 4. Computes leaf hashes
 * for each file 5. Builds a Merkle tree 6. Outputs the root hash to a file
 */
public abstract class GenerateMerkleTask extends DefaultTask {

  // Patterns for files to exclude from integrity computation
  private static final Pattern[] EXCLUDE_PATTERNS = {
    Pattern.compile("^R\\$.*\\.class$"), // R$drawable.class, R$string.class, etc.
    Pattern.compile("^R\\.class$"), // R.class
    Pattern.compile("^BuildConfig\\.class$"), // BuildConfig.class
    Pattern.compile(".*\\$\\$.*\\.class$"), // Dagger/Hilt generated
    Pattern.compile(".*_Factory\\.class$"), // Dagger factories
    Pattern.compile(".*_MembersInjector\\.class$"), // Dagger injectors
    Pattern.compile("^Hilt_.*\\.class$"), // Hilt generated
    Pattern.compile(".*\\.dex$"), // DEX files
  };

  private String merkleRoot;

  /** The variant name (e.g., "debug", "release"). */
  @Input
  public abstract Property<String> getVariantName();

  /** Whether the task is enabled. */
  @Input
  public abstract Property<Boolean> getPluginEnabled();

  /** Hash algorithm to use (default: SHA-256). */
  @Input
  public abstract Property<String> getAlgorithm();

  /** Directory containing compiled .class files. */
  @InputDirectory
  @Optional
  public abstract DirectoryProperty getClassesDirectory();

  /** Output directory for the Merkle root file. */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDirectory();

  @TaskAction
  public void generate() {
    if (!getPluginEnabled().getOrElse(true)) {
      getLogger().lifecycle("GenerateMerkleTask is disabled, skipping");
      return;
    }

    File classesDir = getClassesDirectory().getAsFile().getOrNull();

    if (classesDir == null || !classesDir.exists()) {
      throw new GradleException(
          "Classes directory does not exist: "
              + (classesDir != null ? classesDir.getPath() : "null"));
    }

    getLogger().lifecycle("Generating Merkle root for variant: {}", getVariantName().get());
    getLogger().lifecycle("Classes directory: {}", classesDir.getAbsolutePath());

    String algorithm = getAlgorithm().getOrElse("SHA-256");

    if (!HashUtils.isAlgorithmSupported(algorithm)) {
      throw new GradleException("Unsupported hash algorithm: " + algorithm);
    }

    try {
      List<byte[]> leafHashes = collectAndHashFiles(classesDir, algorithm);

      if (leafHashes.isEmpty()) {
        throw new GradleException(
            "No bytecode files found in "
                + classesDir.getPath()
                + ". Ensure compilation has completed successfully.");
      }

      getLogger().lifecycle("Found {} class files for integrity computation", leafHashes.size());

      // Build Merkle tree
      MerkleTree tree = new MerkleTree(leafHashes, algorithm);
      merkleRoot = tree.getRootHex();

      getLogger().lifecycle("Merkle root computed: {}", merkleRoot);
      getLogger()
          .lifecycle("Tree height: {}, Leaf count: {}", tree.getHeight(), tree.getLeafCount());

      // Write root to file
      writeRootToFile(merkleRoot);

    } catch (IOException e) {
      throw new GradleException("Failed to generate Merkle root: " + e.getMessage(), e);
    }
  }

  /** Collects and hashes all eligible class files. */
  private List<byte[]> collectAndHashFiles(File classesDir, String algorithm) throws IOException {
    List<Path> classFiles = new ArrayList<>();

    try (Stream<Path> walk = Files.walk(classesDir.toPath())) {
      walk.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".class"))
          .filter(this::isNotExcluded)
          .sorted(Comparator.comparing(Path::toString))
          .forEach(classFiles::add);
    }

    List<byte[]> leafHashes = new ArrayList<>();

    for (Path path : classFiles) {
      try {
        byte[] content = Files.readAllBytes(path);
        byte[] hash = HashUtils.hash(content, algorithm);
        leafHashes.add(hash);

        getLogger()
            .debug(
                "Hashed: {} -> {}",
                classesDir.toPath().relativize(path),
                HashUtils.toHex(hash).substring(0, 16) + "...");

      } catch (IOException e) {
        throw new GradleException("Failed to read file: " + path + " - " + e.getMessage(), e);
      }
    }

    return leafHashes;
  }

  /** Checks if a file should be included (not excluded by patterns). */
  private boolean isNotExcluded(Path path) {
    String fileName = path.getFileName().toString();

    for (Pattern pattern : EXCLUDE_PATTERNS) {
      if (pattern.matcher(fileName).matches()) {
        getLogger().debug("Excluding: {}", fileName);
        return false;
      }
    }

    // Also exclude META-INF
    String pathStr = path.toString();
    if (pathStr.contains("META-INF")) {
      getLogger().debug("Excluding META-INF file: {}", fileName);
      return false;
    }

    return true;
  }

  /** Writes the Merkle root to the output file. */
  private void writeRootToFile(String root) throws IOException {
    File outputDir = getOutputDirectory().getAsFile().get();
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    File outputFile = new File(outputDir, "merkle-root.txt");
    Files.writeString(outputFile.toPath(), root);

    getLogger().lifecycle("Merkle root written to: {}", outputFile.getAbsolutePath());
  }

  /** Returns the computed Merkle root. Only available after task execution. */
  @Internal
  public String getMerkleRoot() {
    return merkleRoot;
  }
}

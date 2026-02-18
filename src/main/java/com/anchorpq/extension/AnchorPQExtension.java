package com.anchorpq.extension;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

/**
 * Extension for configuring Anchor PQ Integrity Plugin.
 *
 * <p>Usage in build.gradle:
 *
 * <pre>
 * anchorpq {
 *     enabled = true
 *     algorithm = "SHA-256"
 *     outputFormat = "JSON"
 *     injectBuildConfig = true
 *     version = "1.0.0"
 *     signerFingerprint = "optional-fingerprint"
 *     mlKemEnabled = true
 *     reportingEndpoint = "https://api.example.com/integrity"
 * }
 * </pre>
 */
public abstract class AnchorPQExtension {

  private final Project project;

  public AnchorPQExtension(Project project) {
    this.project = project;

    getEnabled().convention(true);
    getAlgorithm().convention("SHA-256");
    getOutputFormat().convention("JSON");
    getInjectBuildConfig().convention(true);
    getVersion().convention(project.getVersion().toString());
    getMlKemEnabled().convention(false);
    getGenerateRuntimeHelper().convention(true);
  }

  /** Enable or disable the plugin. Default: true */
  public abstract Property<Boolean> getEnabled();

  /**
   * Hash algorithm to use for Merkle tree computation. Supported: SHA-256, SHA-384, SHA-512,
   * SHA3-256, SHA3-512 Default: SHA-256
   */
  public abstract Property<String> getAlgorithm();

  /** Output format for integrity metadata. Supported: JSON, XML Default: JSON */
  public abstract Property<String> getOutputFormat();

  /** Whether to inject MERKLE_ROOT into BuildConfig. Default: true */
  public abstract Property<Boolean> getInjectBuildConfig();

  /** Application version to include in integrity metadata. Default: project version */
  public abstract Property<String> getVersion();

  /** Optional signer fingerprint for enhanced verification. */
  public abstract Property<String> getSignerFingerprint();

  /** Enable ML-KEM (CRYSTALS-Kyber) for secure runtime reporting. Default: false */
  public abstract Property<Boolean> getMlKemEnabled();

  /** Backend endpoint for integrity reporting. */
  public abstract Property<String> getReportingEndpoint();

  /** Whether to generate runtime helper classes. Default: true */
  public abstract Property<Boolean> getGenerateRuntimeHelper();

  /** Custom output directory for generated files. Default: build/anchorpq/{variant}/ */
  public abstract Property<String> getOutputDirectory();

  /**
   * List of file patterns to exclude from integrity computation. Default excludes: R.class,
   * R$*.class, BuildConfig.class
   */
  public abstract Property<String> getExcludePatterns();

  public Project getProject() {
    return project;
  }
}

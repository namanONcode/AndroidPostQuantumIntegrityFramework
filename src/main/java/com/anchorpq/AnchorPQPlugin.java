package com.anchorpq;

import com.anchorpq.extension.AnchorPQExtension;
import com.anchorpq.task.GenerateMerkleTask;
import com.anchorpq.task.GenerateMetadataTask;
import java.io.File;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskProvider;

/**
 * Main plugin class for Anchor PQ Integrity.
 *
 * <p>This plugin computes a deterministic Merkle root of compiled application bytecode, generates
 * integrity metadata, and supports ML-KEM for secure runtime reporting.
 *
 * <p>Plugin ID: io.github.namanoncode.anchorpq
 */
public class AnchorPQPlugin implements Plugin<Project> {

  public static final String PLUGIN_ID = "io.github.namanoncode.anchorpq";
  public static final String EXTENSION_NAME = "anchorpq";
  public static final String TASK_GROUP = "anchorpq";

  @Override
  public void apply(Project project) {
    Logger logger = project.getLogger();
    logger.lifecycle("Applying Anchor PQ Integrity Plugin");

    // Create and configure extension
    AnchorPQExtension extension =
        project.getExtensions().create(EXTENSION_NAME, AnchorPQExtension.class, project);

    // Check if Android plugin is applied
    project
        .getPluginManager()
        .withPlugin(
            "com.android.application",
            plugin -> {
              configureAndroidProject(project, extension, logger);
            });

    project
        .getPluginManager()
        .withPlugin(
            "com.android.library",
            plugin -> {
              configureAndroidProject(project, extension, logger);
            });

    // Fallback for non-Android Java projects
    project
        .getPluginManager()
        .withPlugin(
            "java",
            plugin -> {
              if (!hasAndroidPlugin(project)) {
                configureJavaProject(project, extension, logger);
              }
            });
  }

  private boolean hasAndroidPlugin(Project project) {
    return project.getPluginManager().hasPlugin("com.android.application")
        || project.getPluginManager().hasPlugin("com.android.library");
  }

  private void configureAndroidProject(
      Project project, AnchorPQExtension extension, Logger logger) {
    logger.info("Configuring Anchor PQ for Android project");

    // For Android projects, always configure for standard variants
    // This avoids issues with androidComponents API compatibility
    configureStandardTasks(project, extension, logger, "debug");
    configureStandardTasks(project, extension, logger, "release");
  }

  @SuppressWarnings("unchecked")
  private void configureWithAndroidComponents(
      Project project, AnchorPQExtension extension, Logger logger, Object androidComponents) {
    try {
      // Use Android Gradle Plugin API
      java.lang.reflect.Method onVariantsMethod =
          androidComponents.getClass().getMethod("onVariants", org.gradle.api.Action.class);

      onVariantsMethod.invoke(
          androidComponents,
          (org.gradle.api.Action<Object>)
              variant -> {
                try {
                  java.lang.reflect.Method getNameMethod = variant.getClass().getMethod("getName");
                  String variantName = (String) getNameMethod.invoke(variant);

                  logger.info("Configuring Anchor PQ for variant: " + variantName);
                  configureStandardTasks(project, extension, logger, variantName);

                } catch (Exception e) {
                  logger.error("Error configuring variant: " + e.getMessage());
                }
              });
    } catch (Exception e) {
      logger.warn("Could not use androidComponents API: " + e.getMessage());
      // Fallback to extension-based configuration
      configureWithAndroidExtension(project, extension, logger);
    }
  }

  private void configureWithAndroidExtension(
      Project project, AnchorPQExtension extension, Logger logger) {
    project.afterEvaluate(
        p -> {
          Object android = p.getExtensions().findByName("android");
          if (android != null) {
            try {
              java.lang.reflect.Method getApplicationVariantsMethod =
                  android.getClass().getMethod("getApplicationVariants");
              Object variants = getApplicationVariantsMethod.invoke(android);

              if (variants instanceof Iterable) {
                for (Object variant : (Iterable<?>) variants) {
                  java.lang.reflect.Method getNameMethod = variant.getClass().getMethod("getName");
                  String variantName = (String) getNameMethod.invoke(variant);

                  configureStandardTasks(project, extension, logger, variantName);
                }
              }
            } catch (Exception e) {
              logger.warn("Could not enumerate variants: " + e.getMessage());
              configureStandardTasks(project, extension, logger, "debug");
              configureStandardTasks(project, extension, logger, "release");
            }
          }
        });
  }

  private void configureJavaProject(Project project, AnchorPQExtension extension, Logger logger) {
    logger.info("Configuring Anchor PQ for Java project");
    configureStandardTasks(project, extension, logger, "main");
  }

  private void configureStandardTasks(
      Project project, AnchorPQExtension extension, Logger logger, String variant) {
    String capitalizedVariant = capitalize(variant);

    // Register GenerateMerkleTask
    String merkleTaskName = "generateMerkleRoot" + capitalizedVariant;
    TaskProvider<GenerateMerkleTask> merkleTask =
        project
            .getTasks()
            .register(
                merkleTaskName,
                GenerateMerkleTask.class,
                task -> {
                  task.setGroup(TASK_GROUP);
                  task.setDescription("Generates Merkle root for " + variant + " variant");
                  task.getVariantName().set(variant);
                  task.getPluginEnabled().set(extension.getEnabled());
                  task.getAlgorithm().set(extension.getAlgorithm());
                  task.getOutputDirectory()
                      .set(new File(project.getBuildDir(), "anchorpq/" + variant));

                  // Configure input directory based on project type
                  File classesDir = findClassesDirectory(project, variant);
                  task.getClassesDirectory().set(classesDir);
                });

    // Register GenerateMetadataTask
    String metadataTaskName = "generateIntegrityMetadata" + capitalizedVariant;
    TaskProvider<GenerateMetadataTask> metadataTask =
        project
            .getTasks()
            .register(
                metadataTaskName,
                GenerateMetadataTask.class,
                task -> {
                  task.setGroup(TASK_GROUP);
                  task.setDescription("Generates integrity metadata for " + variant + " variant");
                  task.getVariantName().set(variant);
                  task.getPluginEnabled().set(extension.getEnabled());
                  task.getAlgorithm().set(extension.getAlgorithm());
                  task.getVersion().set(extension.getVersion());
                  task.getSignerFingerprint().set(extension.getSignerFingerprint());
                  task.getOutputDirectory()
                      .set(new File(project.getBuildDir(), "anchorpq/" + variant));
                  task.getMerkleRootFile()
                      .set(
                          new File(
                              project.getBuildDir(), "anchorpq/" + variant + "/merkle-root.txt"));

                  // Depend on merkle task
                  task.dependsOn(merkleTask);
                });

    // Register task to copy integrity.json to assets
    String copyAssetsTaskName = "copyIntegrityAssets" + capitalizedVariant;
    TaskProvider<Task> copyAssetsTask =
        project
            .getTasks()
            .register(
                copyAssetsTaskName,
                task -> {
                  task.setGroup(TASK_GROUP);
                  task.setDescription(
                      "Copies integrity.json to assets for " + variant + " variant");
                  task.dependsOn(metadataTask);

                  task.doLast(
                      t -> {
                        File sourceFile =
                            new File(
                                project.getBuildDir(), "anchorpq/" + variant + "/integrity.json");
                        File assetsDir =
                            new File(
                                project.getBuildDir(),
                                "intermediates/merged_assets/"
                                    + variant
                                    + "/merge"
                                    + capitalizedVariant
                                    + "Assets/out");

                        // Also try alternate path for different AGP versions
                        if (!assetsDir.exists()) {
                          assetsDir =
                              new File(
                                  project.getBuildDir(),
                                  "intermediates/assets/"
                                      + variant
                                      + "/merge"
                                      + capitalizedVariant
                                      + "Assets");
                        }
                        if (!assetsDir.exists()) {
                          assetsDir = new File(project.getProjectDir(), "src/main/assets");
                        }

                        if (!assetsDir.exists()) {
                          assetsDir.mkdirs();
                        }

                        File destFile = new File(assetsDir, "integrity.json");

                        if (sourceFile.exists()) {
                          try {
                            java.nio.file.Files.copy(
                                sourceFile.toPath(),
                                destFile.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            logger.lifecycle("Copied integrity.json to: " + destFile.getPath());
                          } catch (java.io.IOException e) {
                            logger.warn("Failed to copy integrity.json: " + e.getMessage());
                          }
                        } else {
                          logger.warn(
                              "Source integrity.json not found at: " + sourceFile.getPath());
                        }
                      });
                });

    // Configure BuildConfig injection EARLY (before afterEvaluate)
    // This must happen before configurations are resolved
    if (extension.getInjectBuildConfig().get()) {
      configureBuildConfigInjection(project, extension, variant, merkleTask, logger);
    }

    // Hook into compile tasks
    project.afterEvaluate(
        p -> {
          if (!extension.getEnabled().get()) {
            logger.info("Anchor PQ is disabled, skipping task configuration");
            return;
          }

          // Find the appropriate compile task
          String compileTaskName = findCompileTaskName(variant);
          Task compileTask = p.getTasks().findByName(compileTaskName);

          if (compileTask != null) {
            merkleTask.configure(task -> task.dependsOn(compileTask));
            logger.info("Configured {} to run after {}", merkleTaskName, compileTaskName);
          }

          // Hook into assemble task if it exists
          String assembleTaskName = "assemble" + capitalizedVariant;
          Task assembleTask = p.getTasks().findByName(assembleTaskName);

          if (assembleTask != null) {
            assembleTask.dependsOn(metadataTask);
            assembleTask.dependsOn(copyAssetsTask);
            logger.info(
                "Configured {} to depend on {} and {}",
                assembleTaskName,
                metadataTaskName,
                copyAssetsTaskName);
          }

          // Also hook into merge assets task if available
          String mergeAssetsTaskName = "merge" + capitalizedVariant + "Assets";
          Task mergeAssetsTask = p.getTasks().findByName(mergeAssetsTaskName);
          if (mergeAssetsTask != null) {
            mergeAssetsTask.finalizedBy(copyAssetsTask);
            logger.info(
                "Configured {} to be finalized by {}", mergeAssetsTaskName, copyAssetsTaskName);
          }
        });
  }

  private File findClassesDirectory(Project project, String variant) {
    // Try Android-style path first
    File androidPath =
        new File(project.getBuildDir(), "intermediates/javac/" + variant + "/classes");
    if (androidPath.exists()) {
      return androidPath;
    }

    // Try alternative Android path
    File altAndroidPath =
        new File(
            project.getBuildDir(),
            "intermediates/javac/"
                + variant
                + "/compile"
                + capitalize(variant)
                + "JavaWithJavac/classes");
    if (altAndroidPath.exists()) {
      return altAndroidPath;
    }

    // Try standard Java path
    File javaPath = new File(project.getBuildDir(), "classes/java/" + variant);
    if (javaPath.exists()) {
      return javaPath;
    }

    // Default to Android path (will be created during build)
    return androidPath;
  }

  private String findCompileTaskName(String variant) {
    if ("main".equals(variant)) {
      return "compileJava";
    }
    return "compile" + capitalize(variant) + "JavaWithJavac";
  }

  private void configureBuildConfigInjection(
      Project project,
      AnchorPQExtension extension,
      String variant,
      TaskProvider<GenerateMerkleTask> merkleTask,
      Logger logger) {
    // Note: Direct buildConfigField injection is not reliable with modern AGP
    // as it can trigger configuration mutation errors. Instead, we use
    // generated source files which is a more robust approach.

    logger.info("Using generated source file approach for MERKLE_ROOT (more reliable with AGP 8+)");

    // Generate source file as the primary approach for merkle root
    generateIntegritySourceFile(project, variant, merkleTask, logger);
  }

  private void generateIntegritySourceFile(
      Project project, String variant, TaskProvider<GenerateMerkleTask> merkleTask, Logger logger) {
    String generateSourceTaskName = "generateIntegritySource" + capitalize(variant);

    project
        .getTasks()
        .register(
            generateSourceTaskName,
            task -> {
              task.setGroup(TASK_GROUP);
              task.setDescription("Generates IntegrityConfig source file for " + variant);
              task.dependsOn(merkleTask);

              task.doLast(
                  t -> {
                    File outputDir =
                        new File(project.getBuildDir(), "generated/source/anchorpq/" + variant);
                    outputDir.mkdirs();

                    File sourceFile = new File(outputDir, "IntegrityConfig.java");

                    GenerateMerkleTask merkleT = merkleTask.get();
                    String merkleRoot = merkleT.getMerkleRoot();

                    String sourceContent =
                        String.format(
                            "package com.anchorpq.generated;\n\n"
                                + "/**\n"
                                + " * Auto-generated integrity configuration.\n"
                                + " * DO NOT MODIFY - This file is generated by Anchor PQ Integrity Plugin.\n"
                                + " */\n"
                                + "public final class IntegrityConfig {\n"
                                + "    public static final String MERKLE_ROOT = \"%s\";\n"
                                + "    public static final String VARIANT = \"%s\";\n"
                                + "    public static final String ALGORITHM = \"SHA-256\";\n"
                                + "    public static final long BUILD_TIMESTAMP = %dL;\n"
                                + "    \n"
                                + "    private IntegrityConfig() {\n"
                                + "        throw new AssertionError(\"No instances\");\n"
                                + "    }\n"
                                + "}\n",
                            merkleRoot != null ? merkleRoot : "",
                            variant,
                            System.currentTimeMillis());

                    try {
                      java.nio.file.Files.writeString(sourceFile.toPath(), sourceContent);
                      logger.lifecycle(
                          "Generated IntegrityConfig.java at: " + sourceFile.getPath());
                    } catch (java.io.IOException e) {
                      logger.error("Failed to generate IntegrityConfig.java: " + e.getMessage());
                    }
                  });
            });
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return Character.toUpperCase(str.charAt(0)) + str.substring(1);
  }
}

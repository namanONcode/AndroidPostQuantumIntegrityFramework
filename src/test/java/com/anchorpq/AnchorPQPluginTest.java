package com.anchorpq;

import static org.junit.jupiter.api.Assertions.*;

import com.anchorpq.extension.AnchorPQExtension;
import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for AnchorPQPlugin. */
class AnchorPQPluginTest {

  @TempDir File tempDir;

  private Project project;

  @BeforeEach
  void setUp() {
    project = ProjectBuilder.builder().withProjectDir(tempDir).build();
  }

  private void evaluateProject() {
    ((ProjectInternal) project).evaluate();
  }

  @Test
  @DisplayName("Should apply plugin successfully")
  void testPluginApplies() {
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("io.github.namanoncode.anchorpq");

    assertTrue(project.getPlugins().hasPlugin("io.github.namanoncode.anchorpq"));
  }

  @Test
  @DisplayName("Should create extension")
  void testExtensionCreated() {
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("io.github.namanoncode.anchorpq");

    AnchorPQExtension extension = project.getExtensions().findByType(AnchorPQExtension.class);

    assertNotNull(extension);
  }

  @Test
  @DisplayName("Extension should have default values")
  void testExtensionDefaults() {
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("io.github.namanoncode.anchorpq");

    AnchorPQExtension extension = project.getExtensions().findByType(AnchorPQExtension.class);

    assertTrue(extension.getEnabled().get());
    assertEquals("SHA-256", extension.getAlgorithm().get());
    assertEquals("JSON", extension.getOutputFormat().get());
    assertTrue(extension.getInjectBuildConfig().get());
  }

  @Test
  @DisplayName("Should register tasks for Java project")
  void testTasksRegistered() {
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("io.github.namanoncode.anchorpq");

    // Force task registration and evaluation
    evaluateProject();

    assertNotNull(project.getTasks().findByName("generateMerkleRootMain"));
    assertNotNull(project.getTasks().findByName("generateIntegrityMetadataMain"));
  }

  @Test
  @DisplayName("Extension configuration should be respected")
  void testExtensionConfiguration() {
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("io.github.namanoncode.anchorpq");

    AnchorPQExtension extension = project.getExtensions().findByType(AnchorPQExtension.class);

    extension.getEnabled().set(false);
    extension.getAlgorithm().set("SHA-512");
    extension.getVersion().set("2.0.0");

    assertFalse(extension.getEnabled().get());
    assertEquals("SHA-512", extension.getAlgorithm().get());
    assertEquals("2.0.0", extension.getVersion().get());
  }

  @Test
  @DisplayName("Tasks should be in correct group")
  void testTaskGroup() {
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("io.github.namanoncode.anchorpq");
    evaluateProject();

    var merkleTask = project.getTasks().findByName("generateMerkleRootMain");
    assertNotNull(merkleTask);
    assertEquals("anchorpq", merkleTask.getGroup());
  }

  @Test
  @DisplayName("Metadata task should depend on Merkle task")
  void testTaskDependencies() {
    project.getPluginManager().apply("java");
    project.getPluginManager().apply("io.github.namanoncode.anchorpq");
    evaluateProject();

    var metadataTask = project.getTasks().findByName("generateIntegrityMetadataMain");
    assertNotNull(metadataTask);

    boolean dependsOnMerkle =
        metadataTask.getDependsOn().stream()
            .anyMatch(
                dep -> {
                  if (dep instanceof org.gradle.api.tasks.TaskProvider) {
                    return ((org.gradle.api.tasks.TaskProvider<?>) dep)
                        .getName()
                        .equals("generateMerkleRootMain");
                  }
                  return false;
                });

    assertTrue(dependsOnMerkle, "Metadata task should depend on Merkle task");
  }
}

package com.anchorpq.merkle;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for MerkleTree implementation. */
class MerkleTreeTest {

  @Test
  @DisplayName("Should compute consistent root for same inputs")
  void testDeterministicRoot() {
    List<byte[]> leaves =
        Arrays.asList(
            HashUtils.sha256("file1.class".getBytes()),
            HashUtils.sha256("file2.class".getBytes()),
            HashUtils.sha256("file3.class".getBytes()));

    MerkleTree tree1 = new MerkleTree(leaves);
    MerkleTree tree2 = new MerkleTree(leaves);

    assertEquals(tree1.getRootHex(), tree2.getRootHex(), "Same inputs should produce same root");
  }

  @Test
  @DisplayName("Should handle single leaf")
  void testSingleLeaf() {
    List<byte[]> leaves = Collections.singletonList(HashUtils.sha256("single.class".getBytes()));

    MerkleTree tree = new MerkleTree(leaves);

    assertNotNull(tree.getRoot());
    assertEquals(1, tree.getLeafCount());
    assertEquals(1, tree.getHeight());
  }

  @Test
  @DisplayName("Should handle even number of leaves")
  void testEvenLeaves() {
    List<byte[]> leaves =
        Arrays.asList(
            HashUtils.sha256("file1.class".getBytes()),
            HashUtils.sha256("file2.class".getBytes()),
            HashUtils.sha256("file3.class".getBytes()),
            HashUtils.sha256("file4.class".getBytes()));

    MerkleTree tree = new MerkleTree(leaves);

    assertNotNull(tree.getRoot());
    assertEquals(4, tree.getLeafCount());
    // Height: level 0 (4 leaves), level 1 (2 nodes), level 2 (1 root) = 3
    assertEquals(3, tree.getHeight());
  }

  @Test
  @DisplayName("Should handle odd number of leaves by duplicating last")
  void testOddLeaves() {
    List<byte[]> leaves =
        Arrays.asList(
            HashUtils.sha256("file1.class".getBytes()),
            HashUtils.sha256("file2.class".getBytes()),
            HashUtils.sha256("file3.class".getBytes()));

    MerkleTree tree = new MerkleTree(leaves);

    assertNotNull(tree.getRoot());
    assertEquals(3, tree.getLeafCount());
  }

  @Test
  @DisplayName("Should produce different roots for different inputs")
  void testDifferentInputsDifferentRoots() {
    List<byte[]> leaves1 =
        Arrays.asList(
            HashUtils.sha256("file1.class".getBytes()), HashUtils.sha256("file2.class".getBytes()));

    List<byte[]> leaves2 =
        Arrays.asList(
            HashUtils.sha256("file1.class".getBytes()),
            HashUtils.sha256("file3.class".getBytes()) // Different
            );

    MerkleTree tree1 = new MerkleTree(leaves1);
    MerkleTree tree2 = new MerkleTree(leaves2);

    assertNotEquals(
        tree1.getRootHex(), tree2.getRootHex(), "Different inputs should produce different roots");
  }

  @Test
  @DisplayName("Should be order-sensitive")
  void testOrderSensitive() {
    byte[] hash1 = HashUtils.sha256("file1.class".getBytes());
    byte[] hash2 = HashUtils.sha256("file2.class".getBytes());

    MerkleTree tree1 = new MerkleTree(Arrays.asList(hash1, hash2));
    MerkleTree tree2 = new MerkleTree(Arrays.asList(hash2, hash1)); // Reversed

    assertNotEquals(
        tree1.getRootHex(), tree2.getRootHex(), "Different order should produce different roots");
  }

  @Test
  @DisplayName("Should throw exception for null leaves")
  void testNullLeaves() {
    assertThrows(IllegalArgumentException.class, () -> new MerkleTree(null));
  }

  @Test
  @DisplayName("Should throw exception for empty leaves")
  void testEmptyLeaves() {
    assertThrows(IllegalArgumentException.class, () -> new MerkleTree(new ArrayList<>()));
  }

  @Test
  @DisplayName("Should support different algorithms")
  void testDifferentAlgorithms() {
    List<byte[]> leaves =
        Arrays.asList(
            HashUtils.hash("file1.class".getBytes(), "SHA-256"),
            HashUtils.hash("file2.class".getBytes(), "SHA-256"));

    MerkleTree sha256Tree = new MerkleTree(leaves, "SHA-256");

    List<byte[]> sha512Leaves =
        Arrays.asList(
            HashUtils.hash("file1.class".getBytes(), "SHA-512"),
            HashUtils.hash("file2.class".getBytes(), "SHA-512"));
    MerkleTree sha512Tree = new MerkleTree(sha512Leaves, "SHA-512");

    // Different algorithms should produce different length roots
    assertTrue(sha256Tree.getRootHex().length() < sha512Tree.getRootHex().length());
  }

  @Test
  @DisplayName("Should generate valid proof")
  void testProofGeneration() {
    List<byte[]> leaves =
        Arrays.asList(
            HashUtils.sha256("file1.class".getBytes()),
            HashUtils.sha256("file2.class".getBytes()),
            HashUtils.sha256("file3.class".getBytes()),
            HashUtils.sha256("file4.class".getBytes()));

    MerkleTree tree = new MerkleTree(leaves);

    // Get proof for leaf at index 2
    List<MerkleTree.ProofNode> proof = tree.getProof(2);

    assertNotNull(proof);
    assertFalse(proof.isEmpty());
  }

  @Test
  @DisplayName("Should verify valid proof")
  void testProofVerification() {
    byte[] leaf0 = HashUtils.sha256("file1.class".getBytes());
    byte[] leaf1 = HashUtils.sha256("file2.class".getBytes());
    byte[] leaf2 = HashUtils.sha256("file3.class".getBytes());
    byte[] leaf3 = HashUtils.sha256("file4.class".getBytes());

    List<byte[]> leaves = Arrays.asList(leaf0, leaf1, leaf2, leaf3);
    MerkleTree tree = new MerkleTree(leaves);

    // Verify each leaf
    for (int i = 0; i < leaves.size(); i++) {
      List<MerkleTree.ProofNode> proof = tree.getProof(i);
      boolean valid = MerkleTree.verifyProof(leaves.get(i), proof, tree.getRoot(), "SHA-256");
      assertTrue(valid, "Proof for leaf " + i + " should be valid");
    }
  }

  @Test
  @DisplayName("Should reject invalid proof")
  void testInvalidProofRejection() {
    List<byte[]> leaves =
        Arrays.asList(
            HashUtils.sha256("file1.class".getBytes()),
            HashUtils.sha256("file2.class".getBytes()),
            HashUtils.sha256("file3.class".getBytes()),
            HashUtils.sha256("file4.class".getBytes()));

    MerkleTree tree = new MerkleTree(leaves);

    // Get proof for leaf 0
    List<MerkleTree.ProofNode> proof = tree.getProof(0);

    // Try to verify with wrong leaf
    byte[] wrongLeaf = HashUtils.sha256("wrong.class".getBytes());
    boolean valid = MerkleTree.verifyProof(wrongLeaf, proof, tree.getRoot(), "SHA-256");

    assertFalse(valid, "Proof should be invalid for wrong leaf");
  }

  @Test
  @DisplayName("Builder should work correctly")
  void testBuilder() {
    MerkleTree tree =
        MerkleTree.builder()
            .algorithm("SHA-256")
            .addLeaf("file1.class".getBytes())
            .addLeaf("file2.class".getBytes())
            .addLeaf("file3.class".getBytes())
            .build();

    assertNotNull(tree);
    assertEquals(3, tree.getLeafCount());
    assertNotNull(tree.getRootHex());
  }

  @Test
  @DisplayName("Builder should throw exception when no leaves")
  void testBuilderNoLeaves() {
    MerkleTree.Builder builder = MerkleTree.builder();
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  @DisplayName("Should handle large number of leaves")
  void testLargeTree() {
    List<byte[]> leaves = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      leaves.add(HashUtils.sha256(("file" + i + ".class").getBytes()));
    }

    MerkleTree tree = new MerkleTree(leaves);

    assertNotNull(tree.getRoot());
    assertEquals(1000, tree.getLeafCount());
    // Verify root is deterministic
    MerkleTree tree2 = new MerkleTree(leaves);
    assertEquals(tree.getRootHex(), tree2.getRootHex());
  }

  @Test
  @DisplayName("Root hex should be valid hex string")
  void testRootHexFormat() {
    List<byte[]> leaves =
        Arrays.asList(
            HashUtils.sha256("file1.class".getBytes()), HashUtils.sha256("file2.class".getBytes()));

    MerkleTree tree = new MerkleTree(leaves);
    String rootHex = tree.getRootHex();

    // SHA-256 produces 32 bytes = 64 hex chars
    assertEquals(64, rootHex.length());
    assertTrue(rootHex.matches("[0-9a-f]+"), "Root should be lowercase hex");
  }
}

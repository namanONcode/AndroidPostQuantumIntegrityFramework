package com.anchorpq.merkle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of a Merkle Tree for computing integrity roots.
 *
 * <p>Merkle Tree Construction Rules: 1. Pair adjacent hashes 2. If odd number of leaves, duplicate
 * the last leaf 3. Hash concatenation of left + right 4. Continue until single root
 *
 * <p>This implementation is: - Deterministic: same inputs produce same output - Order-sensitive:
 * leaf order matters - Immutable: tree cannot be modified after creation
 */
public final class MerkleTree {

  private final List<byte[]> leaves;
  private final String algorithm;
  private final byte[] root;
  private final List<List<byte[]>> levels;

  /**
   * Creates a new Merkle tree from the given leaf hashes.
   *
   * @param leafHashes list of leaf hashes (must not be empty)
   * @param algorithm hash algorithm to use
   * @throws IllegalArgumentException if leaves are empty or null
   */
  public MerkleTree(List<byte[]> leafHashes, String algorithm) {
    if (leafHashes == null || leafHashes.isEmpty()) {
      throw new IllegalArgumentException("Leaf hashes cannot be null or empty");
    }
    if (algorithm == null || algorithm.isEmpty()) {
      throw new IllegalArgumentException("Algorithm cannot be null or empty");
    }
    if (!HashUtils.isAlgorithmSupported(algorithm)) {
      throw new IllegalArgumentException("Unsupported hash algorithm: " + algorithm);
    }

    this.algorithm = algorithm;
    this.leaves = Collections.unmodifiableList(new ArrayList<>(leafHashes));
    this.levels = new ArrayList<>();
    this.root = computeRoot();
  }

  /**
   * Creates a Merkle tree using SHA-256.
   *
   * @param leafHashes list of leaf hashes
   */
  public MerkleTree(List<byte[]> leafHashes) {
    this(leafHashes, "SHA-256");
  }

  /**
   * Computes the Merkle root from leaf hashes.
   *
   * @return the root hash
   */
  private byte[] computeRoot() {
    List<byte[]> currentLevel = new ArrayList<>(leaves);
    levels.add(Collections.unmodifiableList(new ArrayList<>(currentLevel)));

    while (currentLevel.size() > 1) {
      List<byte[]> nextLevel = new ArrayList<>();

      for (int i = 0; i < currentLevel.size(); i += 2) {
        byte[] left = currentLevel.get(i);
        byte[] right;

        if (i + 1 < currentLevel.size()) {
          right = currentLevel.get(i + 1);
        } else {
          right = left;
        }

        byte[] combined = HashUtils.hashConcat(left, right, algorithm);
        nextLevel.add(combined);
      }

      levels.add(Collections.unmodifiableList(new ArrayList<>(nextLevel)));
      currentLevel = nextLevel;
    }

    return currentLevel.get(0);
  }

  /**
   * Returns the Merkle root as byte array.
   *
   * @return copy of the root hash
   */
  public byte[] getRoot() {
    byte[] copy = new byte[root.length];
    System.arraycopy(root, 0, copy, 0, root.length);
    return copy;
  }

  /**
   * Returns the Merkle root as hexadecimal string.
   *
   * @return hex-encoded root hash
   */
  public String getRootHex() {
    return HashUtils.toHex(root);
  }

  /**
   * Returns the number of leaves in the tree.
   *
   * @return number of leaves
   */
  public int getLeafCount() {
    return leaves.size();
  }

  /**
   * Returns the height of the tree.
   *
   * @return tree height (number of levels)
   */
  public int getHeight() {
    return levels.size();
  }

  /**
   * Returns the hash algorithm used.
   *
   * @return algorithm name
   */
  public String getAlgorithm() {
    return algorithm;
  }

  /**
   * Generates a proof for the given leaf index.
   *
   * @param leafIndex index of the leaf
   * @return list of sibling hashes needed for verification
   * @throws IndexOutOfBoundsException if index is invalid
   */
  public List<ProofNode> getProof(int leafIndex) {
    if (leafIndex < 0 || leafIndex >= leaves.size()) {
      throw new IndexOutOfBoundsException("Invalid leaf index: " + leafIndex);
    }

    List<ProofNode> proof = new ArrayList<>();
    int index = leafIndex;

    for (int level = 0; level < levels.size() - 1; level++) {
      List<byte[]> currentLevel = levels.get(level);
      int siblingIndex;
      boolean isLeft;

      if (index % 2 == 0) {
        siblingIndex = index + 1;
        isLeft = false;
        if (siblingIndex >= currentLevel.size()) {
          siblingIndex = index;
        }
      } else {
        siblingIndex = index - 1;
        isLeft = true;
      }

      byte[] siblingHash = currentLevel.get(siblingIndex);
      proof.add(new ProofNode(siblingHash, isLeft));

      index = index / 2;
    }

    return proof;
  }

  /**
   * Verifies a proof for the given leaf.
   *
   * @param leaf the leaf hash to verify
   * @param proof the proof path
   * @param expectedRoot the expected root hash
   * @return true if the proof is valid
   */
  public static boolean verifyProof(
      byte[] leaf, List<ProofNode> proof, byte[] expectedRoot, String algorithm) {
    if (leaf == null || proof == null || expectedRoot == null) {
      return false;
    }

    byte[] current = leaf;

    for (ProofNode node : proof) {
      byte[] left, right;
      if (node.isLeft()) {
        left = node.getHash();
        right = current;
      } else {
        left = current;
        right = node.getHash();
      }
      current = HashUtils.hashConcat(left, right, algorithm);
    }

    return HashUtils.constantTimeEquals(current, expectedRoot);
  }

  /** Represents a node in a Merkle proof. */
  public static class ProofNode {
    private final byte[] hash;
    private final boolean isLeft;

    public ProofNode(byte[] hash, boolean isLeft) {
      this.hash = hash.clone();
      this.isLeft = isLeft;
    }

    public byte[] getHash() {
      return hash.clone();
    }

    public boolean isLeft() {
      return isLeft;
    }

    public String getHashHex() {
      return HashUtils.toHex(hash);
    }
  }

  /** Builder for creating Merkle trees from raw data. */
  public static class Builder {
    private final List<byte[]> leafHashes = new ArrayList<>();
    private String algorithm = "SHA-256";

    /**
     * Adds raw data as a leaf (will be hashed).
     *
     * @param data raw data to add
     * @return this builder
     */
    public Builder addLeaf(byte[] data) {
      if (data != null) {
        leafHashes.add(HashUtils.hash(data, algorithm));
      }
      return this;
    }

    /**
     * Adds a pre-computed hash as a leaf.
     *
     * @param hash pre-computed hash
     * @return this builder
     */
    public Builder addLeafHash(byte[] hash) {
      if (hash != null) {
        leafHashes.add(hash.clone());
      }
      return this;
    }

    /**
     * Sets the hash algorithm.
     *
     * @param algorithm algorithm name
     * @return this builder
     */
    public Builder algorithm(String algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    /**
     * Builds the Merkle tree.
     *
     * @return the constructed tree
     * @throws IllegalStateException if no leaves were added
     */
    public MerkleTree build() {
      if (leafHashes.isEmpty()) {
        throw new IllegalStateException("Cannot build tree with no leaves");
      }
      return new MerkleTree(leafHashes, algorithm);
    }
  }

  /**
   * Creates a new builder.
   *
   * @return new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }
}

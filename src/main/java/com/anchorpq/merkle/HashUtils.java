package com.anchorpq.merkle;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for cryptographic hash operations.
 *
 * <p>Supports: SHA-256, SHA-384, SHA-512, SHA3-256, SHA3-512
 *
 * <p>Security considerations: - No random salt added for deterministic builds - No timestamps in
 * hash computation - Raw bytes hashed directly
 */
public final class HashUtils {

  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  private HashUtils() {
    throw new AssertionError("No instances");
  }

  /**
   * Computes hash of the given data using SHA-256.
   *
   * @param data the data to hash
   * @return the hash as byte array
   * @throws IllegalStateException if SHA-256 is not available
   */
  public static byte[] sha256(byte[] data) {
    return hash(data, "SHA-256");
  }

  /**
   * Computes hash of the given data using the specified algorithm.
   *
   * @param data the data to hash
   * @param algorithm the hash algorithm (e.g., "SHA-256", "SHA3-256")
   * @return the hash as byte array
   * @throws IllegalStateException if the algorithm is not available
   */
  public static byte[] hash(byte[] data, String algorithm) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    if (algorithm == null || algorithm.isEmpty()) {
      throw new IllegalArgumentException("Algorithm cannot be null or empty");
    }

    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      return digest.digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Hash algorithm not available: " + algorithm, e);
    }
  }

  /**
   * Computes hash of concatenated data.
   *
   * @param left first data block
   * @param right second data block
   * @param algorithm the hash algorithm
   * @return the hash of left + right
   */
  public static byte[] hashConcat(byte[] left, byte[] right, String algorithm) {
    if (left == null || right == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }

    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      digest.update(left);
      digest.update(right);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Hash algorithm not available: " + algorithm, e);
    }
  }

  /**
   * Concatenates two byte arrays.
   *
   * @param a first array
   * @param b second array
   * @return concatenated array
   */
  public static byte[] concat(byte[] a, byte[] b) {
    if (a == null || b == null) {
      throw new IllegalArgumentException("Arrays cannot be null");
    }

    byte[] result = new byte[a.length + b.length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  /**
   * Converts byte array to hexadecimal string.
   *
   * @param bytes the byte array
   * @return hexadecimal representation
   */
  public static String toHex(byte[] bytes) {
    if (bytes == null) {
      return "";
    }

    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
      sb.append(HEX_CHARS[b & 0x0F]);
    }
    return sb.toString();
  }

  /**
   * Converts hexadecimal string to byte array.
   *
   * @param hex the hexadecimal string
   * @return byte array
   * @throws IllegalArgumentException if the string is not valid hex
   */
  public static byte[] fromHex(String hex) {
    if (hex == null || hex.isEmpty()) {
      return new byte[0];
    }

    if (hex.length() % 2 != 0) {
      throw new IllegalArgumentException("Hex string must have even length");
    }

    byte[] result = new byte[hex.length() / 2];
    for (int i = 0; i < result.length; i++) {
      int high = Character.digit(hex.charAt(i * 2), 16);
      int low = Character.digit(hex.charAt(i * 2 + 1), 16);

      if (high == -1 || low == -1) {
        throw new IllegalArgumentException("Invalid hex character at position " + (i * 2));
      }

      result[i] = (byte) ((high << 4) | low);
    }
    return result;
  }

  /**
   * Validates that an algorithm is supported.
   *
   * @param algorithm the algorithm name
   * @return true if supported
   */
  public static boolean isAlgorithmSupported(String algorithm) {
    try {
      MessageDigest.getInstance(algorithm);
      return true;
    } catch (NoSuchAlgorithmException e) {
      return false;
    }
  }

  /**
   * Constant-time comparison of two byte arrays. Prevents timing attacks.
   *
   * @param a first array
   * @param b second array
   * @return true if arrays are equal
   */
  public static boolean constantTimeEquals(byte[] a, byte[] b) {
    if (a == null || b == null) {
      return a == b;
    }

    if (a.length != b.length) {
      return false;
    }

    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }
    return result == 0;
  }
}

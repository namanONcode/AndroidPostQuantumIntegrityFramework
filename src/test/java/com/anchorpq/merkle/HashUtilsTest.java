package com.anchorpq.merkle;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for HashUtils. */
class HashUtilsTest {

  @Test
  @DisplayName("SHA-256 should produce consistent output")
  void testSha256Deterministic() {
    byte[] data = "test data".getBytes();

    byte[] hash1 = HashUtils.sha256(data);
    byte[] hash2 = HashUtils.sha256(data);

    assertArrayEquals(hash1, hash2, "Same input should produce same hash");
  }

  @Test
  @DisplayName("SHA-256 should produce 32 bytes")
  void testSha256Length() {
    byte[] hash = HashUtils.sha256("test".getBytes());
    assertEquals(32, hash.length);
  }

  @Test
  @DisplayName("Different inputs should produce different hashes")
  void testDifferentInputs() {
    byte[] hash1 = HashUtils.sha256("input1".getBytes());
    byte[] hash2 = HashUtils.sha256("input2".getBytes());

    assertFalse(HashUtils.constantTimeEquals(hash1, hash2));
  }

  @Test
  @DisplayName("Empty input should be hashable")
  void testEmptyInput() {
    byte[] hash = HashUtils.sha256(new byte[0]);
    assertNotNull(hash);
    assertEquals(32, hash.length);
  }

  @Test
  @DisplayName("Should throw for null input")
  void testNullInput() {
    assertThrows(IllegalArgumentException.class, () -> HashUtils.sha256(null));
  }

  @Test
  @DisplayName("toHex should produce correct output")
  void testToHex() {
    byte[] bytes = new byte[] {0x00, 0x0F, (byte) 0xAB, (byte) 0xFF};
    String hex = HashUtils.toHex(bytes);

    assertEquals("000fabff", hex);
  }

  @Test
  @DisplayName("fromHex should parse correctly")
  void testFromHex() {
    String hex = "000fabff";
    byte[] bytes = HashUtils.fromHex(hex);

    assertArrayEquals(new byte[] {0x00, 0x0F, (byte) 0xAB, (byte) 0xFF}, bytes);
  }

  @Test
  @DisplayName("toHex and fromHex should be inverse")
  void testHexRoundTrip() {
    byte[] original = HashUtils.sha256("test".getBytes());
    String hex = HashUtils.toHex(original);
    byte[] decoded = HashUtils.fromHex(hex);

    assertArrayEquals(original, decoded);
  }

  @Test
  @DisplayName("Should handle uppercase hex")
  void testUppercaseHex() {
    String hex = "ABCDEF";
    byte[] bytes = HashUtils.fromHex(hex);

    assertArrayEquals(new byte[] {(byte) 0xAB, (byte) 0xCD, (byte) 0xEF}, bytes);
  }

  @Test
  @DisplayName("Should throw for odd-length hex")
  void testOddHexLength() {
    assertThrows(IllegalArgumentException.class, () -> HashUtils.fromHex("abc"));
  }

  @Test
  @DisplayName("Should throw for invalid hex chars")
  void testInvalidHexChars() {
    assertThrows(IllegalArgumentException.class, () -> HashUtils.fromHex("ghij"));
  }

  @Test
  @DisplayName("hashConcat should produce consistent results")
  void testHashConcat() {
    byte[] left = HashUtils.sha256("left".getBytes());
    byte[] right = HashUtils.sha256("right".getBytes());

    byte[] result1 = HashUtils.hashConcat(left, right, "SHA-256");
    byte[] result2 = HashUtils.hashConcat(left, right, "SHA-256");

    assertArrayEquals(result1, result2);
  }

  @Test
  @DisplayName("hashConcat should be order-sensitive")
  void testHashConcatOrderSensitive() {
    byte[] a = HashUtils.sha256("a".getBytes());
    byte[] b = HashUtils.sha256("b".getBytes());

    byte[] ab = HashUtils.hashConcat(a, b, "SHA-256");
    byte[] ba = HashUtils.hashConcat(b, a, "SHA-256");

    assertFalse(HashUtils.constantTimeEquals(ab, ba));
  }

  @Test
  @DisplayName("concat should concatenate correctly")
  void testConcat() {
    byte[] a = new byte[] {1, 2, 3};
    byte[] b = new byte[] {4, 5, 6};
    byte[] result = HashUtils.concat(a, b);

    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6}, result);
  }

  @Test
  @DisplayName("constantTimeEquals should work correctly")
  void testConstantTimeEquals() {
    byte[] a = new byte[] {1, 2, 3};
    byte[] b = new byte[] {1, 2, 3};
    byte[] c = new byte[] {1, 2, 4};
    byte[] d = new byte[] {1, 2};

    assertTrue(HashUtils.constantTimeEquals(a, b));
    assertFalse(HashUtils.constantTimeEquals(a, c));
    assertFalse(HashUtils.constantTimeEquals(a, d));
  }

  @Test
  @DisplayName("constantTimeEquals should handle nulls")
  void testConstantTimeEqualsNull() {
    byte[] a = new byte[] {1, 2, 3};

    assertTrue(HashUtils.constantTimeEquals(null, null));
    assertFalse(HashUtils.constantTimeEquals(a, null));
    assertFalse(HashUtils.constantTimeEquals(null, a));
  }

  @Test
  @DisplayName("Should support multiple algorithms")
  void testMultipleAlgorithms() {
    byte[] data = "test".getBytes();

    assertTrue(HashUtils.isAlgorithmSupported("SHA-256"));
    assertTrue(HashUtils.isAlgorithmSupported("SHA-384"));
    assertTrue(HashUtils.isAlgorithmSupported("SHA-512"));

    byte[] sha256 = HashUtils.hash(data, "SHA-256");
    byte[] sha384 = HashUtils.hash(data, "SHA-384");
    byte[] sha512 = HashUtils.hash(data, "SHA-512");

    assertEquals(32, sha256.length);
    assertEquals(48, sha384.length);
    assertEquals(64, sha512.length);
  }

  @Test
  @DisplayName("Should throw for unsupported algorithm")
  void testUnsupportedAlgorithm() {
    assertFalse(HashUtils.isAlgorithmSupported("INVALID-ALGO"));
    assertThrows(
        IllegalStateException.class, () -> HashUtils.hash("test".getBytes(), "INVALID-ALGO"));
  }

  @Test
  @DisplayName("toHex should handle null")
  void testToHexNull() {
    assertEquals("", HashUtils.toHex(null));
  }

  @Test
  @DisplayName("fromHex should handle empty string")
  void testFromHexEmpty() {
    assertArrayEquals(new byte[0], HashUtils.fromHex(""));
    assertArrayEquals(new byte[0], HashUtils.fromHex(null));
  }
}

package com.anchorpq.runtime;

/**
 * Interface for transmitting integrity reports securely.
 *
 * <p>Implementations should handle: - Network communication - Error handling and retries - Response
 * validation
 *
 * <p>The payload received is already encrypted using ML-KEM.
 */
public interface IntegrityTransport {

  /**
   * Sends an encrypted integrity payload to the backend.
   *
   * @param payload the encrypted payload bytes
   * @throws IntegrityTransportException if transmission fails
   */
  void send(byte[] payload) throws IntegrityTransportException;

  /**
   * Sends an encrypted integrity payload and returns the response.
   *
   * @param payload the encrypted payload bytes
   * @return the server response
   * @throws IntegrityTransportException if transmission fails
   */
  byte[] sendAndReceive(byte[] payload) throws IntegrityTransportException;

  /**
   * Checks if the transport is available.
   *
   * @return true if transport can be used
   */
  boolean isAvailable();

  /** Exception thrown when transport operations fail. */
  class IntegrityTransportException extends Exception {
    public IntegrityTransportException(String message) {
      super(message);
    }

    public IntegrityTransportException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

package com.anchorpq.server.service;

import com.anchorpq.server.config.RateLimitConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiting service based on client IP address.
 *
 * <p>This implementation uses a sliding window approach with per-minute limits. For production
 * deployments, consider using a distributed rate limiter (e.g., Redis-based) for multi-instance
 * support.
 */
@ApplicationScoped
public class RateLimitService {

    @Inject RateLimitConfig rateLimitConfig;

    private final Map<String, RateLimitEntry> clientLimits = new ConcurrentHashMap<>();

    // Cleanup entries older than 2 minutes
    private static final long CLEANUP_THRESHOLD_MS = 2 * 60 * 1000;

    /**
     * Checks if a request from the given client IP is allowed.
     *
     * @param clientIp The client's IP address
     * @return true if the request is allowed, false if rate limited
     */
    public boolean isAllowed(String clientIp) {
        if (!rateLimitConfig.enabled()) {
            return true;
        }

        cleanupOldEntries();

        long currentMinute = System.currentTimeMillis() / 60000;
        int maxRequests = rateLimitConfig.requestsPerMinute();

        RateLimitEntry entry =
                clientLimits.computeIfAbsent(clientIp, k -> new RateLimitEntry(currentMinute));

        // Reset counter if we're in a new minute
        if (entry.minute != currentMinute) {
            entry.minute = currentMinute;
            entry.count.set(0);
            entry.lastAccess = System.currentTimeMillis();
        }

        int currentCount = entry.count.incrementAndGet();
        entry.lastAccess = System.currentTimeMillis();

        if (currentCount > maxRequests) {
            Log.warn(
                    "Rate limit exceeded for client: "
                            + maskIp(clientIp)
                            + " ("
                            + currentCount
                            + "/"
                            + maxRequests
                            + " requests)");
            return false;
        }

        return true;
    }

    /**
     * Gets the remaining requests for a client in the current window.
     *
     * @param clientIp The client's IP address
     * @return Number of remaining requests, or -1 if rate limiting is disabled
     */
    public int getRemainingRequests(String clientIp) {
        if (!rateLimitConfig.enabled()) {
            return -1;
        }

        long currentMinute = System.currentTimeMillis() / 60000;
        RateLimitEntry entry = clientLimits.get(clientIp);

        if (entry == null || entry.minute != currentMinute) {
            return rateLimitConfig.requestsPerMinute();
        }

        return Math.max(0, rateLimitConfig.requestsPerMinute() - entry.count.get());
    }

    /**
     * Resets the rate limit counter for a specific client (for testing).
     *
     * @param clientIp The client's IP address
     */
    public void resetLimit(String clientIp) {
        clientLimits.remove(clientIp);
    }

    /** Clears all rate limit entries (for testing). */
    public void clearAllLimits() {
        clientLimits.clear();
    }

    /** Removes stale entries to prevent memory leaks. */
    private void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        clientLimits
                .entrySet()
                .removeIf(entry -> (now - entry.getValue().lastAccess) > CLEANUP_THRESHOLD_MS);
    }

    /** Masks an IP address for logging (privacy). */
    private String maskIp(String ip) {
        if (ip == null) return "unknown";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".xxx";
        }
        return ip;
    }

    /** Internal class to track rate limit state per client. */
    private static class RateLimitEntry {
        volatile long minute;
        final AtomicInteger count;
        volatile long lastAccess;

        RateLimitEntry(long minute) {
            this.minute = minute;
            this.count = new AtomicInteger(0);
            this.lastAccess = System.currentTimeMillis();
        }
    }
}

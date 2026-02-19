package com.anchorpq.server.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Configuration mapping for rate limiting settings. */
@ConfigMapping(prefix = "anchorpq.ratelimit")
public interface RateLimitConfig {

    /** Enable or disable rate limiting. */
    @WithDefault("true")
    boolean enabled();

    /** Maximum number of requests per minute per client. */
    @WithDefault("60")
    int requestsPerMinute();
}

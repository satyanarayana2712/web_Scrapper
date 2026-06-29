package com.scraper.fetcher;

public class RateLimiter {
    private final com.google.common.util.concurrent.RateLimiter limiter;

    public RateLimiter() {
        this(0.5);
    }

    public RateLimiter(double requestsPerSecond) {
        this.limiter = com.google.common.util.concurrent.RateLimiter.create(requestsPerSecond);
    }

    public void acquire() {
        limiter.acquire();
    }
}

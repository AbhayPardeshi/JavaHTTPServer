package main.java.rateLimiting;

public class TokenBucket {
    private final int capacity;          // max tokens the bucket can hold
    private final double refillRate;     // tokens per second
    private double tokens;               // current tokens (can be fractional)
    private long lastRefillTime;         // last refill timestamp (nanoseconds)

    public TokenBucket(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity; // start full
        this.lastRefillTime = System.nanoTime();
    }

    public synchronized boolean allowRequest() {
        refill();

        if (tokens >= 1) {
            tokens -= 1;
            return true;
        } else {
            return false;
        }
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillTime) / 1_000_000_000.0;

        double refillTokens = elapsedSeconds * refillRate;

        if (refillTokens > 0) {
            tokens = Math.min(capacity, refillTokens + tokens);
            lastRefillTime = now;
        }
    }

    public synchronized double getTokens() {
        return tokens;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getRefillRate() {
        return refillRate;
    }
}

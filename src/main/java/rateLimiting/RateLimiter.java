package main.java.rateLimiting;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private final TokenBucket serverBucket;            // Global bucket
    private final Map<String, TokenBucket> ipBucket;   // Per-IP buckets
    private final Map<String, TokenBucket> pathBucket; // Per-path buckets

    private final int perIpCapacity;
    private final double perIpRefillRate;
    private final int perPathCapacity;
    private final double perPathRefillRate;

    public RateLimiter(int serverCapacity, double serverRefillRate,
                       int perIpCapacity, double perIpRefillRate,
                       int perPathCapacity, double perPathRefillRate) {

        this.serverBucket = new TokenBucket(serverCapacity, serverRefillRate);
        this.ipBucket = new ConcurrentHashMap<>();
        this.pathBucket = new ConcurrentHashMap<>();

        this.perIpCapacity = perIpCapacity;
        this.perIpRefillRate = perIpRefillRate;
        this.perPathCapacity = perPathCapacity;
        this.perPathRefillRate = perPathRefillRate;
    }

    public boolean checkRequest(String path, String ip) {
        if (!serverBucket.allowRequest()) return false;

        return checkIp(ip) && checkPath(path);
    }

    private boolean checkIp(String ipAddress) {
        TokenBucket bucket = ipBucket.computeIfAbsent(ipAddress,
                k -> new TokenBucket(perIpCapacity, perIpRefillRate));
        return bucket.allowRequest();
    }

    private boolean checkPath(String path) {
        TokenBucket bucket = pathBucket.computeIfAbsent(path,
                k -> new TokenBucket(perPathCapacity, perPathRefillRate));
        return bucket.allowRequest();
    }

    // -------------------- GETTERS --------------------
    public TokenBucket getServerBucket() {
        return serverBucket;
    }

    public Map<String, TokenBucket> getIpBucket() {
        return ipBucket;
    }

    public Map<String, TokenBucket> getPathBucket() {
        return pathBucket;
    }

    public double getGlobalTokens() {
        return serverBucket.getTokens();
    }

    public double getIpTokens(String ip) {
        TokenBucket bucket = ipBucket.get(ip);
        return bucket != null ? bucket.getTokens() : 0;
    }

    public double getPathTokens(String path) {
        TokenBucket bucket = pathBucket.get(path);
        return bucket != null ? bucket.getTokens() : 0;
    }

    // -------------------- RATE LIMIT HEADERS --------------------
    public static void writeRateLimitHeaders(PrintWriter out, RateLimiter rateLimiter, String ip, String path) {
        // Global bucket
        int globalLimit = rateLimiter.getServerBucket().getCapacity();
        double globalRemaining = rateLimiter.getGlobalTokens();
        double globalReset = (globalLimit - globalRemaining) / rateLimiter.getServerBucket().getRefillRate();

        // Per-IP bucket
        TokenBucket ipBucket = rateLimiter.getIpBucket().get(ip);
        int ipLimit = ipBucket != null ? ipBucket.getCapacity() : 0;
        double ipRemaining = ipBucket != null ? ipBucket.getTokens() : 0;
        double ipReset = ipBucket != null ? (ipLimit - ipRemaining) / ipBucket.getRefillRate() : 0;

        // Per-path bucket
        TokenBucket pathBucket = rateLimiter.getPathBucket().get(path);
        int pathLimit = pathBucket != null ? pathBucket.getCapacity() : 0;
        double pathRemaining = pathBucket != null ? pathBucket.getTokens() : 0;
        double pathReset = pathBucket != null ? (pathLimit - pathRemaining) / pathBucket.getRefillRate() : 0;

        // Conservative choice: remaining = min, reset = max
        double remaining = Math.min(globalRemaining, Math.min(ipRemaining, pathRemaining));
        double reset = Math.max(Math.max(globalReset, ipReset), pathReset);

        // Write headers
        out.println("X-RateLimit-Limit: " + Math.max(globalLimit, Math.max(ipLimit, pathLimit)));
        out.println("X-RateLimit-Remaining: " + (int) remaining);
        out.println("X-RateLimit-Reset: " + (int) Math.ceil(reset));
    }
}

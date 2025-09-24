package main.java.connectionManagement;

import java.net.Socket;

public class Connection implements Comparable<Connection> {
    public Socket clientSocket;
    public int priority;
    public String clientIP;
    public long arrivalTime;

    public Connection(Socket clientSocket, String clientIP) {
        this.clientSocket = clientSocket;
        this.clientIP = clientIP;
        this.arrivalTime = System.nanoTime();
        this.priority = Integer.MAX_VALUE; // default until set
    }

    // Assigns priority based on request path
    public void setPriority(String path) {
        if (path.startsWith("/api")) {
            priority = 1;
        } else if (path.startsWith("/index")) {
            priority = 2;
        } else {
            priority = 3;
        }
    }

    @Override
    public int compareTo(Connection other) {
        // Lower priority value = higher priority
        int cmp = Integer.compare(this.priority, other.priority);
        if (cmp == 0) {
            // Tie-breaker: earlier arrival wins
            return Long.compare(this.arrivalTime, other.arrivalTime);
        }
        return cmp;
    }
}

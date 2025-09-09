package main.java.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ServerConfig {
    private final int port;
    private final int threads;
    private final int maxConnections;
    private final int connectionTimeout;

    public ServerConfig(String filename)  {
        Properties props = new Properties();

        // try-with-resources - we do not need to explicitly write finally block to close
        Path configPath = Paths.get("src/main/resources", filename);
        if (!Files.exists(configPath)) {
            // Fallback to current directory
            configPath = Paths.get(filename);
        }
        try (FileInputStream file = new FileInputStream(configPath.toString())) {
            props.load(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.port = parseInt(props, "port", 8080); // default: 8080
        this.threads = parseInt(props, "threads", 10);
        this.maxConnections = parseInt(props, "maxConnections", 50);
        this.connectionTimeout = parseInt(props, "connectionTimeout", 5000);
    }

    private int parseInt(Properties props, String key, int defaultValue){
        String value = props.getProperty(key);

        if(value == null){
            return defaultValue;
        }

        try{
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid value for " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    public int getPort() {
        return port;
    }

    public int getThreads() {
        return threads;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

}

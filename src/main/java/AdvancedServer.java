package main.java;


import main.java.cache.BloomFilter;
import main.java.cache.HybridCache;
import main.java.config.ServerConfig;
import main.java.http.HTTPRequest;
import main.java.http.Router;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AdvancedServer {

    private final ServerConfig config;
    private Router router;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private Socket clientSocket;
    private ExecutorService threadPool;
    private BloomFilter filter;
    private HybridCache cache;

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    public final AtomicInteger totalConnections = new AtomicInteger(0);
    private final String PUBLIC_DIR = "src/main/resources/staticFiles";

    public AdvancedServer(ServerConfig config){
        this.config = config;
    }

    private void start() throws IOException {

        serverSocket = new ServerSocket(config.getPort());
        isRunning = true;
        System.out.println("Server is running at port: "+config.getPort());

        // create {threadPoolSize} threadPool and make them custom named using serverThreadFactory
        threadPool = Executors.newFixedThreadPool(config.getThreads(), new ServerThreadFactory());

        // initialize the cache and bloomFilter
        filter = new BloomFilter(1024 * 1024, 3);// 1MB bit array, 3 hash functions
        File publicDir = new File(PUBLIC_DIR);
        for (File f : Objects.requireNonNull(publicDir.listFiles())) {
            if (f.isFile()) {
                String relativePath = "/" + f.getName();  // or how your router defines paths
                filter.add(relativePath);
            }
        }

        cache = new HybridCache(2,filter);
        while(isRunning){
            try{
                clientSocket = serverSocket.accept();
                totalConnections.incrementAndGet();
                System.out.println("client connected");
                threadPool.submit(new ManagedClientHandler(clientSocket));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class ServerThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override

        public Thread newThread(Runnable r){
            Thread t = new Thread(r, "AdvancedServer-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    private class ManagedClientHandler implements Runnable {
        Socket clientSocket;
        public ManagedClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            activeConnections.incrementAndGet();
            String clientAddress = clientSocket.getRemoteSocketAddress().toString();
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                HTTPRequest request = new HTTPRequest();
                //request.parseHTTPRequest(in);
                request.FSMParser(in);
                OutputStream outStream = clientSocket.getOutputStream();
                PrintWriter out = new PrintWriter(outStream, true);

                router = new Router();
                router.route(request,out,outStream,cache);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
                activeConnections.decrementAndGet();
                System.out.println("Disconnected " + clientAddress +
                        " (Active: " + activeConnections.get() + ")");
            }

        }
    }

    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null) {
            serverSocket.close();
        }

        threadPool.shutdown(); // stop accepting new tasks

        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow(); // force shutdown
                System.out.println("Forcefully shut down tasks.");
            } else {
                System.out.println("Executor shut down gracefully.");
                System.out.println();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Cache Stats:");
        System.out.println("Hits: " + cache.getHits());
        System.out.println("Misses: " + cache.getMisses());
        System.out.println("Evictions: " + cache.getEvictions());
    }

    private static void printUsage() {
        System.out.println("Usage: java AdvancedManagedServer [OPTIONS]");
        System.out.println("Options:");
        System.out.println("  --port <port>              Server port (default: 8080)");
        System.out.println("  --threads <count>          Thread pool size (default: 10)");
        System.out.println("  --max-connections <count>  Max concurrent connections (default: 20)");
        System.out.println("  --timeout <ms>             Connection timeout in milliseconds (default: 30000)");
        System.out.println("  --help                     Show this help message");
    }

    public static void main(String[] args) {

        ServerConfig config = new ServerConfig("config.properties");
        AdvancedServer server = new AdvancedServer(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();

            } catch (IOException e) {
                System.err.println("Error stopping server: " + e.getMessage());
            }
        }));

        try{
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

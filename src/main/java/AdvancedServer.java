package main.java;


import main.java.config.ServerConfig;
import main.java.http.HTTPRequest;
import main.java.http.Router;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    public final AtomicInteger totalConnections = new AtomicInteger(0);

    public AdvancedServer(ServerConfig config){
        this.config = config;
    }

    private void start() throws IOException {

        serverSocket = new ServerSocket(config.getPort());
        isRunning = true;
        System.out.println("Server is running at port: "+config.getPort());

        // create {threadPoolSize} threadPool and make them custom named using serverThreadFactory
        threadPool = Executors.newFixedThreadPool(config.getThreads(), new ServerThreadFactory());

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
                request.parseHTTPRequest(in);
                OutputStream outStream = clientSocket.getOutputStream();
                PrintWriter out = new PrintWriter(outStream, true);
                router = new Router();
                router.route(request,out,outStream);

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
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
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

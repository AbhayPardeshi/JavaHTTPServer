package main.java.http;

import main.java.handlers.HelloHandler;
import main.java.handlers.RouteHandler;
import main.java.handlers.TimeHandler;
import main.java.handlers.UserHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Router {

    private StaticRouter staticRoutes;
    private TrieRouter dynamicRoutes;

    public Router() {
        this.staticRoutes = new StaticRouter();
        this.dynamicRoutes = new TrieRouter();
        addDynamicRoutes();
    }

    private void addDynamicRoutes(){
        Properties props = new Properties();

        String filename = "dynamicRoutes.properties";
        Path configPath = Paths.get("src/main/resources", filename);
        if (!Files.exists(configPath)) {
            configPath = Paths.get(filename);
        }
        try (FileInputStream file = new FileInputStream(configPath.toString())) {
            props.load(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("hi");
        for (String key : props.stringPropertyNames()) {
            String path = props.getProperty(key);
            RouteHandler handler = switch (key) {
                case "users" -> new UserHandler();      // dynamic route handler
                //case "comments" -> new CommentsHandler();
                //case "about" -> new AboutHandler();
                default -> throw new RuntimeException("Unknown handler key: " + key);
            };

            dynamicRoutes.setPath(path, handler);
        }
    }

//    public void route(HTTPRequest request, PrintWriter out, OutputStream rawOut){
//        String pathKey = request.getPath();
//
//        if (dynamicRoutes.containsPath(pathKey)) {
//            //dynamicRoutes.get(pathKey).handle(request, out);
//            return;
//        }
//
//        String filePath = staticRoutes.getPath(pathKey);
//
//        // Serve static file
//        staticRoutes.serveStaticFile(filePath, out, rawOut);
//
//    }

    public void route(HTTPRequest request, PrintWriter out, OutputStream rawOut) {
        String path = request.getPath();

        // 1️⃣ Check dynamic routes first
        TrieRouter.RouteMatch match = dynamicRoutes.findPath(path);
        if (match != null) {
            // inject params into request (optional if your request supports it)
            request.setParams(match.params());

            // call handler
            match.handler().handle(request, out);
            return;
        }

        // 2️⃣ Otherwise check static routes
        String filePath = staticRoutes.getPath(path);
        if (filePath != null) {
            staticRoutes.serveStaticFile(filePath, out, rawOut);
            return;
        }

        // 3️⃣ Nothing matched → 404
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/plain");
        out.println();
        out.println("404 Not Found: " + path);
    }


}

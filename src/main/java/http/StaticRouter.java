package main.java.http;

import main.java.cache.HybridCache;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class StaticRouter {
    private final Map<String, String> staticRoutes;
    private final String PUBLIC_DIR = "src/main/resources/staticFiles";

    public StaticRouter(){
        this.staticRoutes = new ConcurrentHashMap<>();
        addStaticRoutes();
    }

    public String getPath(String key){
        return staticRoutes.get(key);
    }

    private void addStaticRoutes() {
        Properties props = new Properties();

        String filename = "staticRoutes.properties";
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

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String path = (String) entry.getKey();
            String file = (String) entry.getValue();
            staticRoutes.put(path, file);
        }
    }

//       without cache and bloom filter
//    public void serveStaticFile(String path, PrintWriter out,OutputStream rawOut) {
//        File file = new File(PUBLIC_DIR + path);
//
//        if (!file.exists() || file.isDirectory()) {
//            send404(out);
//            return;
//        }
//        sendFileResponse(file, out,rawOut);
//    }

    public void serveStaticFile(String path, PrintWriter out, OutputStream rawOut, HybridCache cache) {
        //  Read from disk
        File file = new File(PUBLIC_DIR + path);

        // Check Bloom filter first
        if (!cache.getFilter().mightContain(path)) {
            send404(out); // definitely not present
            return;
        }

        // Check HybridCache
        byte[] data = cache.getFile(path);
        if (data != null) {
            sendFileResponse(file, out, rawOut, data); // cache hit
            return;
        }

        if (!file.exists() || file.isDirectory()) {
            send404(out);
            return;
        }

        try {
            data = Files.readAllBytes(file.toPath());
            cache.putFile(path, data); // store in cache
            sendFileResponse(file,out, rawOut, data); // send to client
        } catch (IOException e) {
            e.printStackTrace();
            send500(out,e.getMessage());
        }
    }



    private void sendFileResponse(File file, PrintWriter out,OutputStream rawOut,byte[] data) {
        try {
            String mimeType = Files.probeContentType(Paths.get(file.getAbsolutePath()));
            if (mimeType == null) {
                mimeType = "text/plain";
            }

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + mimeType + "; charset=UTF-8");
            out.println("Content-Length: " + file.length());
            out.println();
            out.flush();

//            BufferedReader reader = new BufferedReader(new FileReader(file));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                out.println(line);
//            }
//            reader.close();

//            FileInputStream fis = new FileInputStream(file);
//            byte[] buffer = new byte[8192];
//            int bytesRead;
//            while ((bytesRead = fis.read(buffer)) != -1) {
//                rawOut.write(buffer, 0, bytesRead);
//            }
//            rawOut.flush();
//            fis.close();

            rawOut.write(data);
            rawOut.flush();

        } catch (IOException e) {
            send500(out, e.getMessage());
        }
    }

    private void send500(PrintWriter out, String message) {
        out.println("HTTP/1.1 500 Internal Server Error");
        out.println("Content-Type: text/html; charset=UTF-8");
        out.println();
        out.println("<h1>500 Internal Server Error</h1>");
        out.println("<p>" + message + "</p>");
    }

    private void send404(PrintWriter out) {
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/html; charset=UTF-8");
        out.println();
        out.println("<h1>404 Not Found</h1>");
    }
}

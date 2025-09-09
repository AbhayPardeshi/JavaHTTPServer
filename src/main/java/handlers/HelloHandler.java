package main.java.handlers;

import main.java.http.HTTPRequest;
import java.io.PrintWriter;

public class HelloHandler implements RouteHandler {
    @Override
    public void handle(HTTPRequest request, PrintWriter out) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/plain; charset=UTF-8");
        out.println();
        out.println("Hello from dynamic route!");
    }
}

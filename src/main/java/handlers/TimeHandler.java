package main.java.handlers;

import main.java.http.HTTPRequest;

import java.io.PrintWriter;

public class TimeHandler implements RouteHandler {
    @Override
    public void handle(HTTPRequest request, PrintWriter out) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json; charset=UTF-8");
        out.println();
        out.println("{ \"time\": \"" + java.time.LocalDateTime.now() + "\" }");
    }
}

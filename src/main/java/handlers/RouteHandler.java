package main.java.handlers;

import main.java.http.HTTPRequest;

import java.io.PrintWriter;

public interface RouteHandler {
    void handle(HTTPRequest request, PrintWriter out);
}

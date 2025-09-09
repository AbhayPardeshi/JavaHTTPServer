package main.java.handlers;

import main.java.http.HTTPRequest;

import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserHandler implements RouteHandler{
    private final Map<Integer,String> users;

    public UserHandler() {
        users = new HashMap<>();

        users.put(1, "Alice");
        users.put(2, "Bob");
        users.put(3, "Charlie");
        users.put(4, "David");
        users.put(5, "Eva");
        users.put(6, "Frank");
        users.put(7, "Grace");
        users.put(8, "Hannah");
        users.put(9, "Ivan");
        users.put(10, "Julia");
    }
    @Override
    public void handle(HTTPRequest request, PrintWriter out) {
        // Extract dynamic param from request (assume request has a getParams() method)
        System.out.println("hi from usersRouter");
        Map<String, String> params = request.getParams();
        String userIdStr = params.get("userId");

        try {
            int userId = Integer.parseInt(userIdStr.substring(1));
            System.out.println(userId);
            String userName = users.get(userId);


            if (userName != null) {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json; charset=UTF-8");
                out.println();
                out.println("Fetched user: ID=" + userId + ", Name=" + userName);
            } else {
                out.println("User not found!");
            }

        } catch (NumberFormatException e) {
            out.println("Invalid user ID!");
        }
    }
}

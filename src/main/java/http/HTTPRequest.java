package main.java.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
    private String method;
    private String path;
    private String httpVersion;
    private Map<String, String> headers;
    private String body;

    public HTTPRequest() {
        this.headers = new HashMap<>();
    }

    public void parseHTTPRequest(BufferedReader input) throws IOException {

        String inputLine = input.readLine();

        String[] str = inputLine.split(" ");
        this.setMethod(str[0]);
        this.setPath(str[1]);
        this.setHttpVersion(str[2]);

        String nextLine = input.readLine();
        while ( nextLine != null && !nextLine.isEmpty()) {
            String[] headerParts = nextLine.split(":", 2);

            String name = headerParts[0].trim();
            String value = headerParts[1].trim();
            this.setHeaders(name,value);


            nextLine = input.readLine();
        }

        String contentLengthValue = this.headers.get("Content-Length");

        if(contentLengthValue != null){
            int contentLength = Integer.parseInt(contentLengthValue);

            char[] bodyChars = new char[contentLength];

            int readSoFar = 0;

            while (readSoFar < contentLength){
                int readNow = input.read(bodyChars,readSoFar,contentLength - readSoFar);
                if (readNow == -1) {
                    break; // Stream ended early
                }
                readSoFar += readNow;
            }

            String body = new String(bodyChars,0,readSoFar);
            this.setBody(body);
        }

    }



    // Getters and setters
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpVersion() {
        return httpVersion;
    }
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
    public void setHeaders(String name, String value) {
        this.headers.put(name, value);
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

}



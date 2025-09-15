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
    private Map<String,String> params;

    public HTTPRequest() {
        this.headers = new HashMap<>();
        this.params = new HashMap<>();
    }

    public void parseHTTPRequest(BufferedReader input) throws IOException {

        String inputLine = input.readLine();
        if (inputLine == null || inputLine.isEmpty()) {
            throw new IOException("Empty HTTP request");
        }

        String[] str = inputLine.split(" ");
        this.setMethod(str[0]);
        this.setPath(str[1]);
        this.setHttpVersion(str[2]);

        String nextLine = input.readLine();
        while ( nextLine != null && !nextLine.isEmpty()) {
            String[] headerParts = nextLine.split(":", 2);

            if (headerParts.length == 2) {
                String name = headerParts[0].trim();
                String value = headerParts[1].trim();
                this.setHeaders(name, value);
            }

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

    enum parseState {
        METHOD, PATH, VERSION, HEADERS, BODY, DONE, ERROR
    }

    public void FSMParser(BufferedReader input) throws IOException {

        parseState currentState = parseState.METHOD;

        while (currentState != parseState.DONE && currentState != parseState.ERROR) {
            String inputLine = input.readLine();

            switch (currentState) {

                case METHOD:
                    if (inputLine == null || inputLine.isEmpty()) {
                        currentState = parseState.ERROR;
                    } else {
                        // Parse request line: METHOD PATH VERSION
                        String[] parts = inputLine.split(" ");
                        if (parts.length < 3) {
                            currentState = parseState.ERROR;
                            break;
                        }
                        this.setMethod(parts[0]);
                        this.setPath(parts[1]);
                        this.setHttpVersion(parts[2]);
                        currentState = parseState.HEADERS;
                    }
                    break;

                case HEADERS:
                    if (inputLine == null) {
                        currentState = parseState.ERROR;
                        break;
                    }

                    if (inputLine.isEmpty()) {
                        // End of headers
                        if (headers.containsKey("content-length") || headers.containsKey("transfer-encoding")) {
                            currentState = parseState.BODY;
                        } else {
                            currentState = parseState.DONE; // no body
                        }
                        break;
                    }

                    // Parse header line
                    String[] headerParts = inputLine.split(":", 2);
                    if (headerParts.length == 2) {
                        String name = headerParts[0].trim().toLowerCase(); // normalize
                        String value = headerParts[1].trim();
                        this.setHeaders(name, value);
                    }
                    break;

                case BODY:
                    // Handle Content-Length
                    if (headers.containsKey("content-length")) {
                        String lengthValue = headers.get("content-length");
                        int contentLength = Integer.parseInt(lengthValue);

                        char[] bodyChars = new char[contentLength];
                        int readSoFar = 0;
                        while (readSoFar < contentLength) {
                            int readNow = input.read(bodyChars, readSoFar, contentLength - readSoFar);
                            if (readNow == -1) break; // Stream ended early
                            readSoFar += readNow;
                        }

                        this.setBody(new String(bodyChars, 0, readSoFar));
                        currentState = parseState.DONE;

                    }
                    // Handle chunked transfer-encoding
                    else if (headers.containsKey("transfer-encoding") &&
                            headers.get("transfer-encoding").equalsIgnoreCase("chunked")) {

                        // Example chunked body:
                        // Request body sent as chunks (hex size + data):
                        // 7\r\n        <- 7 characters in this chunk (hex 7 = 7 decimal)
                        // Mozilla\r\n  <- chunk data
                        // 9\r\n        <- 9 characters in this chunk (hex 9 = 9 decimal)
                        // Developer\r\n
                        // 7\r\n
                        // Network\r\n
                        // 0\r\n        <- 0-length chunk signals end of body
                        // \r\n         <- final CRLF after last chunk


                        StringBuilder bodyBuilder = new StringBuilder();

                        while (true) {
                            String chunkSizeLine = input.readLine();
                            if (chunkSizeLine == null) {
                                currentState = parseState.ERROR;
                                break;
                            }

                            int chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16);

                            if (chunkSize == 0) {
                                input.readLine(); // consume final CRLF after zero-size chunk
                                break; // all chunks read
                            }

                            char[] chunkData = new char[chunkSize];
                            int readSoFar = 0;
                            while (readSoFar < chunkSize) {
                                int readNow = input.read(chunkData, readSoFar, chunkSize - readSoFar);
                                if (readNow == -1) {
                                    currentState = parseState.ERROR;
                                    break;
                                }
                                readSoFar += readNow;
                            }

                            bodyBuilder.append(chunkData, 0, readSoFar);

                            input.readLine(); // consume CRLF after each chunk
                        }

                        this.setBody(bodyBuilder.toString());
                        currentState = parseState.DONE;

                    } else {
                        // No body present
                        currentState = parseState.DONE;
                    }
                    break;
            }
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

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public void addParam(String key, String value) {
        this.params.put(key, value);
    }

}



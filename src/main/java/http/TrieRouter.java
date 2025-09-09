package main.java.http;

import main.java.handlers.RouteHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class TrieRouter {
    private final TrieNode node;
    public TrieRouter(){
        this.node = new TrieNode();
    }



    public void setPath(String path,RouteHandler handler ){

        String[] segments = path.substring(1).split("/");
        TrieNode current = node;

       for(String segment : segments){
           if(segment.isEmpty()) {
               continue;
           }

           if(segment.startsWith(":")){
               if (current.getParamChild() == null) {
                   TrieNode paramNode = new TrieNode();
                   current.setParamChild(paramNode);
                   paramNode.setParamName(segment.substring(1));
               }

               current = current.getParamChild();
           }else{
               if(!current.getChildren().containsKey(segment)){
                   current.addChild(segment);

               }
               current = current.getChildren().get(segment);
           }

       }
        current.setEndOfRoute(true);
        current.setHandler(handler);
    }

    public RouteMatch findPath(String path){
        Map<String,String> params = new HashMap<>();
        String[] segments = path.substring(1).split("/");
        TrieNode current = node;

        for(String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            if(current.getChildren().containsKey(segment)){
                current = current.getChildren().get(segment);
            }else if (current.getParamChild() != null){
                params.put(current.getParamChild().getParamName(),segment);
                current = current.getParamChild();
            }else {
                return null;
            }

        }

        if (current.isEndOfRoute()){
            return new RouteMatch(current.getHandler(), params);
        }

        return null;
    }


    // record class used
    public record RouteMatch(RouteHandler handler, Map<String, String> params) {
    }
}



class TrieNode{
    private Map<String, TrieNode> children;
    private TrieNode paramChild;
    private String paramName;
    private boolean isEndOfRoute;
    private RouteHandler handler;

    public TrieNode() {
        this.children = new ConcurrentHashMap<>();
        this.paramChild = null;
        this.paramName = null;
        this.isEndOfRoute = false;
        this.handler = null;
    }

//    public void addChildren(String name){
//        this.children.put(name,new TrieNode());
//    }

    public void addChild(String name) {
        this.children.computeIfAbsent(name, k -> new TrieNode());
    }


    public void setParamChild(TrieNode paramChild) {
        this.paramChild = paramChild;
    }

    public void setChildren(Map<String, TrieNode> children) {
        this.children = children;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public void setEndOfRoute(boolean endOfRoute) {
        isEndOfRoute = endOfRoute;
    }

    public void setHandler(RouteHandler handler) {
        this.handler = handler;
    }

    public Map<String, TrieNode> getChildren() {
        return children;
    }

    public TrieNode getParamChild() {
        return paramChild;
    }

    public String getParamName() {
        return paramName;
    }

    public boolean isEndOfRoute() {
        return isEndOfRoute;
    }

    public RouteHandler getHandler() {
        return handler;
    }
}
package com.example.mangaapp.API_MangaDex;

import java.util.List;
import java.util.Map;

public class TagResponse {
    private String result;
    private String response;
    private List<Data> data;
    private int limit;
    private int offset;
    private int total;

    public String getResult() { return result; }
    public String getResponse() { return response; }
    public List<Data> getData() { return data; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
    public int getTotal() { return total; }

    public static class Data {
        private String id;
        private String type;
        private Attributes attributes;
        private List<Relationship> relationships;

        public String getId() { return id; }
        public String getType() { return type; }
        public Attributes getAttributes() { return attributes; }
        public List<Relationship> getRelationships() { return relationships; }
    }

    public static class Attributes {
        private Map<String, String> name;
        private Map<String, String> description;
        private String group;
        private int version;

        public Map<String, String> getName() { return name; }
        public Map<String, String> getDescription() { return description; }
        public String getGroup() { return group; }
        public int getVersion() { return version; }
    }

    public static class Relationship {
        private String id;
        private String type;
        private Object attributes;

        public String getId() { return id; }
        public String getType() { return type; }
        public Object getAttributes() { return attributes; }
    }
}


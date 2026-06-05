package com.opengrid.simplerelationshiptracker.model;

public class Event {
    public String id;
    public String name;
    public long timestamp;

    public Event(String id, String name, long timestamp) {
        this.id = id;
        this.name = name;
        this.timestamp = timestamp;
    }
}
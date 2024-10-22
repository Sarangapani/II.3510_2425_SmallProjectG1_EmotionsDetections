package com.example.moodanalyzerapp;

public class ChatMessage {
    private String message;    // The text of the chat message
    private String sender;     // The identifier of the sender (e.g., user ID or name)
    private long timestamp;    // The time the message was sent

    // Constructor to initialize the chat message object
    public ChatMessage(String message, String sender, long timestamp) {
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    // Getter for the message text
    public String getMessage() {
        return message;
    }

    // Getter for the sender's identifier
    public String getSender() {
        return sender;
    }

    // Getter for the timestamp
    public long getTimestamp() {
        return timestamp;
    }
}
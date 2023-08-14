package org.example;

public class Message {

    private int id;
    private String text;
    private String sender;
    private String receiver;

    public Message(String text, String sender, String receiver) {
        this.id = 0;
        this.text = text;
        this.sender = sender;
        this.receiver = receiver;
    }

    public Message() {
    }

    public String getText() {
        return text;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setId(int id) {
        this.id = id;
    }

}



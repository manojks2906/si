package com.companyx.domain;


import java.io.Serializable;

public class AppMessageResponse implements Serializable {

    private String id;

    private String message;

    private Status status;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        SUCCESS,
        FAILURE;
    }
}

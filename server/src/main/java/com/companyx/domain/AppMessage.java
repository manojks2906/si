package com.companyx.domain;

import java.io.Serializable;

public class AppMessage implements Serializable {

    private String id;

    private String message;

    @Override
    public String toString() {
        return new String(id + ", " + message);
    }

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
}

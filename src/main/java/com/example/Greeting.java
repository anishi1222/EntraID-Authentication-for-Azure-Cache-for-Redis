package com.example;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Greeting {
    String name;
    String message;

    public Greeting(String _name, String _message) {
        this.name = _name;
        this.message = _message;
    }

    public Greeting() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

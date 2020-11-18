package com.example.demo;

public class Parser {

    private final String cf1;
    private final String cf2;
    private final String cf3;

    public Parser(String cf1, String cf2, String cf3) {
        this.cf1 = cf1;
        this.cf2 = cf2;
        this.cf3 = cf3;
    }

    public String getCf1() {
        return cf1;
    }

    public String getCf2() {
        return cf2;
    }

    public String getCf3() {
        return cf3;
    }
}

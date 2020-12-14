package com.example.demo;

public class CreateAd {

    private final String responseJSON;

    public CreateAd(String responseJSON) {
        this.responseJSON = responseJSON;
    }

    public String getCf1() {
        return responseJSON;
    }


}

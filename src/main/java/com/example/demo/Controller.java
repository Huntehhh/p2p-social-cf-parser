package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @GetMapping("/2parser")
    public Parser parser(@RequestParam(value = "cf") String cf) {
        String[] strArr = cf.split(",", 2);
        return new Parser(strArr[0], strArr[1]);
    }
}

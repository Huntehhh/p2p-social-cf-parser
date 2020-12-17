package com.example.demo;

import com.facebook.ads.sdk.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class CfParser {

    Methods m = new Methods();

    @PostMapping("/postTesting")
    @ResponseStatus(HttpStatus.CREATED)
    public void createSm(@RequestBody String bodyString) throws JSONException {
        //JSONObject json = m.convertToJson(bodyString);

    }

    @GetMapping("/2parser")
    public String parser(@RequestParam(value = "cf") String cf) {
        String[] strArr = cf.split(",", 999);
        int i = 1;
        JSONObject json = new JSONObject();

        for(String s : strArr) {
            json.put("cf" + i, s);
            i++;
        }
        return json.toString();
    }
}

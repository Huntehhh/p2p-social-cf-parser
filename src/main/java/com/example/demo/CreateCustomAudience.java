package com.example.demo;

import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APIException;
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.CustomAudience;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreateCustomAudience {

    Methods m = new Methods();

    @PostMapping("/createCa")
    @ResponseStatus(HttpStatus.CREATED)
    public String createCa(@RequestBody String bodyString) throws APIException {
        System.out.println(bodyString);
        JSONObject json = m.convertToJson(bodyString);
        String appSecret = "42bbf536e4b868acf3a8d3a912023bb3";
        String name = json.get("name").toString();
        String adAcctId = json.get("ad_acct_id").toString();
        String accessToken = json.get("access_token").toString();
        APIContext context = new APIContext(accessToken, appSecret).enableDebug(false);
        AdAccount adAccount = new AdAccount("act_" + adAcctId, context);

        adAccount.createCustomAudience()
                .setName(name)
                .setSubtype(CustomAudience.EnumSubtype.VALUE_CUSTOM)
                .setDescription("Auto Generated")
                .setCustomerFileSource(CustomAudience.EnumCustomerFileSource.VALUE_USER_PROVIDED_ONLY)
                .execute();

        return "Success";
    }
}

package com.example.demo;

import com.facebook.ads.sdk.APIException;
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.AdCreative;
import com.facebook.ads.sdk.AdImage;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Methods {

    public String getAccessToken() {
        return "EAACr7ptMGuIBACMJo64HofqwTCyXJpa0XlI5cz0AbrriNYYtcnldO0Kvh7hxT0ZBposWVSqkPMYlOVA7tLNBq0kTzCzP3VhD2c1i6gbMVRDOA6NMLjUTnZBIlrPaSZAdnjWyHV1xCGLzucLzMirYIV9S3LhbgUEiAYxq9ZBxcHI7GHEobpDB";
    }

    //Convert String to JSON
    public JSONObject convertToJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject;
        }catch (JSONException err){
            err.printStackTrace();
        }
        return null;
    }

    //This method is to attempt to get the ids associated with ad strategy in the case where we do not need to create them
    public Map<String, String> getIds(String campaignList, String adSetList, String campaignName, String adSetName) {
        Map<String, String> mappedIds = new HashMap<String, String>();
        if(campaignList.contains(campaignName)) {
            mappedIds.put("campaignid", campaignList.substring(campaignList.indexOf(campaignName) - 31, campaignList.indexOf(campaignName)).replaceAll("[^0-9]", ""));
        }
        if(adSetList.contains(adSetName)) {
            mappedIds.put("adsetid", adSetList.substring(adSetList.indexOf(adSetName) - 31, adSetList.indexOf(adSetName)).replaceAll("[^0-9]", ""));
        }
        return mappedIds;
    }

    //This method is to handle the Creative for the Card Ad
    public AdCreative doCardCreative(AdAccount adAccount, String pageId, String creativeName, String toptext,
                                 String imageUrl, String title, String subtitle, String buttontext,
                                 String buttonurl) throws APIException, IOException {
        //Download File from URL
        java.net.URL url = new java.net.URL(imageUrl);
        BufferedImage img = ImageIO.read(url);
        String fileExtension = imageUrl.substring(imageUrl.lastIndexOf("."));
        File pic = new File("temp2" + fileExtension);
        ImageIO.write(img, "png", pic);

        //Upload File to Facebook
        AdImage adImage = adAccount.createAdImage()
                .addUploadFile(creativeName, pic)
                .execute();

        //Insert Variables into JSON
        String ogJSON = "{\"message\":{\"attachment\":{\"type\":\"template\",\"payload\":{\"template_type\":\"generic\",\"elements\":[{\"title\":\"(title)\",\"subtitle\":\"(subtitle)\",\"buttons\":[{\"type\":\"web_url\",\"title\":\"(buttontext)\",\"url\":\"(buttonurl)\"}],\"image_hash\":\"(img_hash)\"}]}},\"text\":\"(toptext)\"}}";
        String smJSON = ogJSON
                .replace("(toptext)", toptext)
                .replace("(img_hash)", adImage.getFieldHash().toString())
                .replace("(title)", title)
                .replace("(subtitle)", subtitle)
                .replace("(buttontext)", buttontext)
                .replace("(buttonurl)", buttonurl);

        //Create Ad Creative
        AdCreative adCreative = adAccount.createAdCreative()
                .setActorId(pageId)
                .setObjectId(pageId)
                .setName(creativeName)
                .setMessengerSponsoredMessage(smJSON)
                .execute();

        return adCreative;
    }

    //This method is to handle the Creative for the Text Ad
    public AdCreative doTextCreative(AdAccount adAccount, String pageId, String creativeName, String toptext, String buttontext, String buttonurl) throws APIException, IOException {
        //Insert Variables into JSON

        String ogJSON = "{\"message\":{\"attachment\":{\"type\":\"template\",\"payload\":{\"template_type\":\"button\",\"text\":\"(toptext)\",\"buttons\":[{\"type\":\"web_url\",\"title\":\"(buttontext)\",\"url\":\"(buttonurl)\"}]}}}}";
        String smJSON = ogJSON
                .replace("(toptext)", toptext)
                .replace("(buttontext)", buttontext)
                .replace("(buttonurl)", buttonurl);

        //Create Ad Creative
        AdCreative adCreative = adAccount.createAdCreative()
                .setActorId(pageId)
                .setObjectId(pageId)
                .setName(creativeName)
                .setMessengerSponsoredMessage(smJSON)
                .execute();

        return adCreative;
    }

    //Convert Date to UNIX
    private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy,kk:mm", Locale.ENGLISH);
    public long unixTimeConversion(String time) {
        long unixTime = 0;
        dateFormat.setTimeZone(TimeZone.getTimeZone("EST"));
        try {
            unixTime = dateFormat.parse(time).getTime();
            unixTime = unixTime / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return unixTime;
    }

    //Calculate Adset Budget
    public Long calculateBudget(Double audienceSize) {
        Long budget = 0L;
        if(!(audienceSize <= 10000)) {
            Double bud = (audienceSize/10000)*120;
            budget = Double.valueOf(bud).longValue();
        } else {
            budget = 120L;
        }
        return budget;
    }
}

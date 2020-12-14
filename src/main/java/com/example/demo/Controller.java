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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
public class Controller {

    @PostMapping("/testing")
    @ResponseStatus(HttpStatus.CREATED)
    public void createSm(@RequestBody String bodyString) throws JSONException {
        JSONObject json = convertToJson(bodyString);

        System.out.println(json.get("lol"));
        System.out.println(json.get("lol2"));
    }

    @GetMapping("/2parser")
    public Parser parser(@RequestParam(value = "cf") String cf) {
        String[] strArr = cf.split(",", 2);
        return new Parser(strArr[0], strArr[1]);
    }
    @PostMapping("/createSm")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAd createAd(@RequestBody String bodyString) throws APIException, IOException, JSONException {
        JSONObject json = convertToJson(bodyString);
        APIContext context = new APIContext(json.get("accesstoken").toString(), json.get("appsecret").toString()).enableDebug(false);
        AdAccount adAccount = new AdAccount("act_" + json.get("adacctid"), context);
        String campaignList = adAccount.getCampaigns().requestNameField().execute().toString();
        String adSetList = adAccount.getAdSets().requestNameField().execute().toString();
        String campaignName = json.get("ngo")  + " - SM";

        Map<String, String> ids = getIds(campaignList, adSetList, campaignName, json.get("adsetname").toString());

        //Check to see if we need to initialize an ad campaign
        if(!campaignList.contains(campaignName)) {
            //Create Campaign
            Campaign adCampaign = adAccount.createCampaign()
                    .setName(campaignName)
                    .setObjective(Campaign.EnumObjective.VALUE_MESSAGES)
                    .setLifetimeBudget(99999900L)
                    .setPacingType(Arrays.asList("no_pacing"))
                    .setStatus(Campaign.EnumStatus.VALUE_PAUSED)
                    .setParam("special_ad_categories", "NONE")
                    .execute();
            ids.put("campaignid", adCampaign.getId().toString());
        }

        //Check to see if we need to initialize a new adSet based on adSetName
        if(!adSetList.contains(json.get("adsetname").toString())) {
            //Create AdSet
            AdSet adSet = adAccount.createAdSet()
                    .setBillingEvent(AdSet.EnumBillingEvent.VALUE_IMPRESSIONS)
                    .setOptimizationGoal(AdSet.EnumOptimizationGoal.VALUE_IMPRESSIONS)
                    .setBidAmount(json.get("bidamount").toString())
                    .setCampaignId(ids.get("campaignid"))
                    .setName(json.get("adsetname").toString())
                    .setStartTime(json.get("adsetstarttime").toString())
                    .setEndTime(json.get("adsetendtime").toString())
                    .setTargeting(
                            new Targeting()
                                    .setFieldCustomAudiences("[{id:" + json.get("customaudienceid") + "}]")
                                    .setFieldPublisherPlatforms(Arrays.asList("messenger"))
                                    .setFieldMessengerPositions(Arrays.asList("sponsored_messages"))
                    )
                    .setStatus(AdSet.EnumStatus.VALUE_PAUSED)
                    .setPromotedObject("{\"page_id\":\"" + json.get("pageid") +"\"}")
                    .execute();
            ids.put("adsetid", adSet.getId().toString());
        }

        //Do Creative
        AdCreative creative = doCreative(adAccount, json.get("pageid").toString(), json.get("adname").toString(), json.get("adtext").toString(),
                json.get("adcardimage").toString(), json.get("adcardtitle").toString(), json.get("adcardsubtitle").toString(),
                json.get("buttontext").toString(), json.get("buttonurl").toString());

        //Create Ad
        adAccount.createAd()
                .setName(json.get("adname").toString())
                .setAdsetId(ids.get("adsetid").toString())
                .setCreative(creative)
                .setStatus(Ad.EnumStatus.VALUE_ACTIVE)
                .execute();

        return new CreateAd("Successfuly Created Ad");
    }


    //This method is used to parse the parameters we received into key:value pairs
    public Map<String, String> parseParams(String params) {
        Map<String, String> mappedParams = new HashMap<String, String>();
        String[] pairs = params.split(", ");
        for (int i=0;i<pairs.length;i++) {
            String pair = pairs[i];
            String[] keyValue = pair.split("=");
            mappedParams.put(keyValue[0].toLowerCase(), keyValue[1]);
        }
        return mappedParams;
    }

    //This method is to attempt to get the ids associated with ad strategy in the case where we do not need to create them
    public Map<String, String> getIds(String campaignList, String adSetList, String campaignName, String adSetName) {
        Map<String, String> mappedIds = new HashMap<String, String>();
        if(campaignList.contains(campaignName)) {
            mappedIds.put("campaignid", campaignList.substring(campaignList.indexOf(campaignName) - 31, campaignList.indexOf(campaignName)).replaceAll("[^0-9]", ""));
        }
        if(adSetList.contains(adSetName)) {
            mappedIds.put("adsetid", adSetList.substring(adSetList.indexOf(campaignName) - 31, adSetList.indexOf(campaignName)).replaceAll("[^0-9]", ""));
        }
        return mappedIds;
    }

    //This method is to handle the Creative for the Ad
    public AdCreative doCreative(AdAccount adAccount, String pageId, String creativeName, String toptext,
                                 String imageUrl, String title, String subtitle, String buttontext,
                                 String buttonurl) throws APIException, IOException {
        //Download File from URL
        java.net.URL url = new java.net.URL(imageUrl);
        BufferedImage img = ImageIO.read(url);
        File pic = new File("temp.jpg");
        ImageIO.write(img, "jpg", pic);

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

    public JSONObject convertToJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject;
        }catch (JSONException err){
            System.out.println("JSON EXCEPTION LOL!!!!!");
        }
        return null;
    }
}

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
public class Controller {

    @PostMapping("/postTesting")
    @ResponseStatus(HttpStatus.CREATED)
    public void createSm(@RequestBody String bodyString) throws JSONException {
        //JSONObject json = convertToJson(bodyString);

        System.out.println(unixTimeConversion("06-10-2021,02:00"));
    }

    @GetMapping("/2parser")
    public Parser parser(@RequestParam(value = "cf") String cf) {
        String[] strArr = cf.split(",", 2);
        return new Parser(strArr[0], strArr[1]);
    }

    @PostMapping("/createCardSm")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAd createAd(@RequestBody String bodyString) throws APIException, IOException, JSONException {
        //Convert request body to JSON object
        JSONObject json = convertToJson(bodyString);

        //Declare Variables Pulled from JSON
        String ngo = json.get("ngo").toString();
        String pageId = json.get("page_id").toString();
        String adAcctId = json.get("ad_acct_id").toString();
        String accessToken = json.get("access_token").toString();
        String bidAmount = json.get("bid_amount").toString();
        String adsetName = json.get("adset_name").toString();
        String customAudienceId = json.get("custom_audience_id").toString();
        String adName = json.get("ad_name").toString();
        String adText = json.get("ad_text").toString();
        String adCardImage = json.get("ad_card_image").toString();
        String adCardTitle = json.get("ad_card_title").toString();
        String adCardSubtitle = json.get("ad_card_subtitle").toString();
        String buttonText = json.get("button_text").toString();
        String buttonUrl = json.get("button_url").toString();
        Long startTime = unixTimeConversion(json.get("adset_start_time").toString());
        Long endTime = unixTimeConversion(json.get("adset_end_time").toString());

        //Do Setup
        APIContext context = new APIContext(accessToken, "42bbf536e4b868acf3a8d3a912023bb3").enableDebug(false);
        AdAccount adAccount = new AdAccount("act_" + adAcctId, context);
        String campaignList = adAccount.getCampaigns().requestNameField().execute().toString();
        String adSetList = adAccount.getAdSets().requestNameField().execute().toString();
        Map<String, String> ids = getIds(campaignList, adSetList, ngo  + " - SM", adsetName);
        APINodeList<CustomAudience> customAudience = CustomAudience.fetchByIds(Collections.singletonList(customAudienceId), Arrays.asList(new String[]{"name", "description", "account_id", "opt_out_link", "approximate_count"}), context);
        Double customAudienceSize = customAudience.get(0).getFieldApproximateCount().doubleValue();

        //Check to see if we need to initialize an ad campaign
        if(!campaignList.contains(ngo + " - SM")) {
            //Create Campaign
            Campaign adCampaign = adAccount.createCampaign()
                    .setName(ngo + " - SM")
                    .setObjective(Campaign.EnumObjective.VALUE_MESSAGES)
                    .setStatus(Campaign.EnumStatus.VALUE_ACTIVE)
                    .setParam("special_ad_categories", "NONE")
                    .execute();
            ids.put("campaignid", adCampaign.getId().toString());
        }

        //Check to see if we need to initialize a new adSet based on adSetName
        if(!adSetList.contains(adsetName)) {
            //Create AdSet
            AdSet adSet = adAccount.createAdSet()
                    .setBillingEvent(AdSet.EnumBillingEvent.VALUE_IMPRESSIONS)
                    .setOptimizationGoal(AdSet.EnumOptimizationGoal.VALUE_IMPRESSIONS)
                    .setBidAmount(bidAmount)
                    .setLifetimeBudget(calculateBudget((double) customAudienceSize) + "00")
                    .setPacingType(Arrays.asList("no_pacing"))
                    .setCampaignId(ids.get("campaignid"))
                    .setName(adsetName)
                    .setStartTime(startTime.toString())
                    .setEndTime(endTime.toString())
                    .setTargeting(
                            new Targeting()
                                    .setFieldCustomAudiences("[{id:" + customAudienceId + "}]")
                                    .setFieldPublisherPlatforms(Arrays.asList("messenger"))
                                    .setFieldMessengerPositions(Arrays.asList("sponsored_messages"))
                    )
                    .setStatus(AdSet.EnumStatus.VALUE_PAUSED)
                    .setPromotedObject("{\"page_id\":\"" + pageId +"\"}")
                    .execute();
            ids.put("adsetid", adSet.getId().toString());
        }

        //Create Ad
        AdCreative creative = doCreative(adAccount, pageId, adName, adText, adCardImage, adCardTitle, adCardSubtitle, buttonText, buttonUrl);
        adAccount.createAd()
                .setName(adName)
                .setAdsetId(ids.get("adsetid").toString())
                .setCreative(creative)
                .setStatus(Ad.EnumStatus.VALUE_ACTIVE)
                .execute();

        return new CreateAd("Successfully Created SM");
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

    //This method is to handle the Creative for the Ad
    public AdCreative doCreative(AdAccount adAccount, String pageId, String creativeName, String toptext,
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

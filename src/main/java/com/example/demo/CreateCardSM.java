package com.example.demo;

import com.facebook.ads.sdk.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@RestController
public class CreateCardSM {

    Methods m = new Methods();

    @PostMapping("/createCardSm")
    @ResponseStatus(HttpStatus.CREATED)
    public String createCardSm(@RequestBody String bodyString) throws APIException, IOException, JSONException {
        //Convert request body to JSON object
        JSONObject json = m.convertToJson(bodyString);
        json.put("access_token", m.getAccessToken());

        //Declare Variables Pulled from JSON
        String ngo = json.get("ngo").toString();
        String pageId = json.get("page_id").toString();
        String adAcctId = json.get("ad_acct_id").toString();
        String accessToken = json.get("access_token").toString();
        String bidAmount = json.get("bid_amount").toString() + "00";
        String adsetName = json.get("adset_name").toString();
        String customAudienceId = json.get("custom_audience_id").toString();
        String adName = json.get("ad_name").toString();
        String adText = json.get("ad_text").toString();
        String adCardImage = json.get("ad_card_image").toString();
        String adCardTitle = json.get("ad_card_title").toString();
        String adCardSubtitle = json.get("ad_card_subtitle").toString();
        String buttonText = json.get("button_text").toString();
        String buttonUrl = json.get("button_url").toString();
        Long startTime = m.unixTimeConversion(json.get("adset_start_time").toString());
        Long endTime = m.unixTimeConversion(json.get("adset_end_time").toString());

        //Do Setup
        APIContext context = new APIContext(accessToken, "42bbf536e4b868acf3a8d3a912023bb3").enableDebug(false);
        AdAccount adAccount = new AdAccount("act_" + adAcctId, context);
        String campaignList = adAccount.getCampaigns().requestNameField().execute().toString();
        String adSetList = adAccount.getAdSets().requestNameField().execute().toString();
        Map<String, String> ids = m.getIds(campaignList, adSetList, ngo  + " - SM", adsetName);
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
                    .setLifetimeBudget(m.calculateBudget((double) customAudienceSize) + "00")
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
                    .setStatus(AdSet.EnumStatus.VALUE_ACTIVE)
                    .setPromotedObject("{\"page_id\":\"" + pageId +"\"}")
                    .execute();
            ids.put("adsetid", adSet.getId().toString());
        }

        //Create Ad
        AdCreative creative = m.doCardCreative(adAccount, pageId, adName, adText, adCardImage, adCardTitle, adCardSubtitle, buttonText, buttonUrl);
        adAccount.createAd()
                .setName(adName)
                .setAdsetId(ids.get("adsetid").toString())
                .setCreative(creative)
                .setStatus(Ad.EnumStatus.VALUE_ACTIVE)
                .execute();

        return "Successfully Created SM";
    }
}

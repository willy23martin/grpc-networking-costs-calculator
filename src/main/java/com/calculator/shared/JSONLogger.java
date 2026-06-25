package com.calculator.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.services.pricing.model.GetPriceListFileUrlResponse;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;
import software.amazon.awssdk.services.pricing.model.ListPriceListsResponse;
import software.amazon.awssdk.services.pricing.model.PriceList;

import java.util.logging.Logger;

public class JSONLogger {

    public static void logAsJSON(Logger log, GetProductsResponse response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();

            rootNode.put("formatVersion", response.formatVersion());

            ArrayNode priceListArray = mapper.createArrayNode();
            for (String priceStr : response.priceList()) {
                priceListArray.add(mapper.readTree(priceStr));
            }
            rootNode.set("priceList", priceListArray);

            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            log.info("AWS Pricing API has been used for rates: \n" + prettyJson);

        } catch (Exception e) {
            log.warning("Failed to format AWS response as beautiful JSON, printing raw format." + e);
            log.info("AWS Pricing API raw fallback: \n" + response);
        }
    }

    public static void logAsJSON(Logger log, ListPriceListsResponse response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();

            if (response.sdkHttpResponse() != null) {
                rootNode.put("statusCode", response.sdkHttpResponse().statusCode());
            }
            rootNode.put("nextToken", response.nextToken());

            ArrayNode priceListsArray = mapper.createArrayNode();
            if (response.hasPriceLists() && response.priceLists() != null) {
                for (PriceList priceList : response.priceLists()) {
                    ObjectNode itemNode = mapper.createObjectNode();

                    // Track standard available native fields on the PriceList domain model
                    itemNode.put("priceListArn", priceList.priceListArn());
                    itemNode.put("regionCode", priceList.regionCode());
                    itemNode.put("currencyCode", priceList.currencyCode());

                    priceListsArray.add(itemNode);
                }
            }
            rootNode.set("priceLists", priceListsArray);

            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            log.info("AWS Price List Bulk API has been checked for offers: \n" + prettyJson);

        } catch (Exception e) {
            log.warning("Failed to format AWS Bulk response as beautiful JSON, printing raw format. " + e);
            log.info("AWS Price List Bulk API raw fallback: \n" + response);
        }
    }

    public static void logAsJSON(Logger log, GetPriceListFileUrlResponse response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();

            if (response.sdkHttpResponse() != null) {
                rootNode.put("statusCode", response.sdkHttpResponse().statusCode());
            }

            // Map the direct download URL returned by the Bulk endpoint
            rootNode.put("url", response.url());

            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            log.info("AWS Price List Bulk API has resolved the download link: \n" + prettyJson);

        } catch (Exception e) {
            log.warning("Failed to format AWS Price List URL response as beautiful JSON, printing raw format. " + e);
            log.info("AWS Price List URL raw fallback: \n" + response);
        }
    }

}

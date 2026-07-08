package com.calculator.shared;

import com.calculator.domain.dto.requests.CloudInfrastructureTotalCostRequest;
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

    public static void logAsJSON(Logger log, CloudInfrastructureTotalCostRequest req) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();

            rootNode.put("albMonthlyCostUsd", req.albMonthlyCostUsd);
            rootNode.put("cacheMonthlyCostUsd", req.cacheMonthlyCostUsd);
            rootNode.put("databaseMonthlyCostUsd", req.databaseMonthlyCostUsd);
            rootNode.put("securityMonthlyCostUsd", req.securityMonthlyCostUsd);
            rootNode.put("containerMonthlyCostUsd", req.containerMonthlyCostUsd);
            rootNode.put("apiGatewayMonthlyCostUsd", req.apiGatewayMonthlyCostUsd);
            rootNode.put("ec2ReplicaMonthlyCostUsd", req.ec2ReplicaMonthlyCostUsd);
            rootNode.put("finopsMonthlySavingUsd", req.finopsMonthlySavingUsd);

            double totalNetCost = req.albMonthlyCostUsd
                    + req.cacheMonthlyCostUsd
                    + req.databaseMonthlyCostUsd
                    + req.securityMonthlyCostUsd
                    + req.containerMonthlyCostUsd
                    + req.apiGatewayMonthlyCostUsd
                    + req.ec2ReplicaMonthlyCostUsd
                    - req.finopsMonthlySavingUsd;

            rootNode.put("calculatedTotalNetCostUsd", totalNetCost);

            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            log.info("Cloud Infrastructure Total Request Metrics: \n" + prettyJson);

        } catch (Exception e) {
            log.warning("Failed to format Cloud Infra Request as beautiful JSON, printing raw format. " + e);
            log.info("Cloud Infra Request raw fallback: \n" + req);
        }
    }

}

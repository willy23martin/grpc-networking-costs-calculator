package com.calculator.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

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

}

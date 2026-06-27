package com.calculator.application.services.calculators.cost.cloud.database.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.DatabaseCostCalculator;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class AWSAuroraDatabaseCostCalculator extends AWSCloudCalculator implements DatabaseCostCalculator  {

    private static final Logger log = Logger.getLogger(AWSAuroraDatabaseCostCalculator.class.getName());

    @Override
    public Map<String, Object> calculateDatabaseBackupPricing() {
        Map<String, Object> databaseBackupCosts = new LinkedHashMap<>();
        mapAuroraReplicaPerReplicaPerHourCosts(databaseBackupCosts);
        return databaseBackupCosts;
    }

    private void mapAuroraReplicaPerReplicaPerHourCosts(Map<String, Object> databaseBackupCosts) {
        databaseBackupCosts.put("auroraReplicaPerHour", fetchAuroraReplicaPrice());
        log.info("Aurora Replica Per Hour Costs have been mapped.");
    }

    private double fetchAuroraReplicaPrice() {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonRDS")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("databaseEngine").value("Aurora MySQL").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value("db.r6g.large").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("productFamily").value("Database Instance").build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();

            GetProductsResponse resp = pricingClient.getProducts(req);
            log.info("GetProductsResponse database " + resp);
            JSONLogger.logAsJSON(log, resp);

            if (resp.priceList() == null || resp.priceList().isEmpty()) {
                log.warning("AWS API returned empty price list for Aurora Replica. Using fallback.");
                return 0.26;
            }

            JsonNode root = mapper.readTree(resp.priceList().getFirst());
            JsonNode onDemand = root.path("terms").path("OnDemand");
            if (onDemand.isMissingNode() || onDemand.isEmpty()) return 0.26;

            JsonNode termValue = onDemand.elements().next();
            JsonNode priceDimensions = termValue.path("priceDimensions");
            if (priceDimensions.isMissingNode() || priceDimensions.isEmpty()) return 0.26;

            JsonNode dimension = priceDimensions.elements().next();
            return dimension.path("pricePerUnit").path("USD").asDouble(0.26);

        } catch (Exception e) {
            log.warning("fetchAuroraReplicaPrice failed: " + e.getMessage());
            return 0.26;
        }
    }

}

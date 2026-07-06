package com.calculator.application.services.calculators.cost.cloud.database.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.DatabaseCostCalculator;
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
public class AWSDynamoDBCostCalculator extends AWSCloudCalculator implements DatabaseCostCalculator {

    private static final Logger log = Logger.getLogger(AWSDynamoDBCostCalculator.class.getName());

    @Override
    public Map<String, Object> calculateDatabaseBackupPricing() {
        Map<String, Object> databaseBackupCosts = new LinkedHashMap<>();
        mapDynamoDBGlobalTablesReplicatedWriteCosts(databaseBackupCosts);
        return databaseBackupCosts;
    }

    private void mapDynamoDBGlobalTablesReplicatedWriteCosts(Map<String, Object> databaseBackupCosts) {
        double replicatedWritePrice = fetchDynamoDBGlobalTableWriteReplicationUnitsPrice();

        databaseBackupCosts.put("dynamoGlobalTablePerWruUsd", replicatedWritePrice);
        databaseBackupCosts.put("dynamoGlobalTableNote",
                String.format(java.util.Locale.US, "Add ~$%.6f/WRU per extra replication region beyond the primary.", replicatedWritePrice));
        log.info("Dynamo DB Global Tables Replicated Write Costs have been mapped dynamically.");
    }

    private double fetchDynamoDBGlobalTableWriteReplicationUnitsPrice() {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonDynamoDB")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("group").value("DynamoDB-ReplicatedWriteUnits").build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();

            GetProductsResponse resp = pricingClient.getProducts(req);
            log.info("GetProductsResponse DynamoDB: " + resp);

            if (resp.priceList() == null || resp.priceList().isEmpty()) {
                log.warning("AWS API returned empty price list for DynamoDB rWRU. Using fallback.");
                return 0.000975;
            }

            JsonNode root = mapper.readTree(resp.priceList().getFirst());
            JsonNode onDemand = root.path("terms").path("OnDemand");
            if (onDemand.isMissingNode() || onDemand.isEmpty()) return 0.000975;

            JsonNode termValue = onDemand.elements().next();
            JsonNode priceDimensions = termValue.path("priceDimensions");
            if (priceDimensions.isMissingNode() || priceDimensions.isEmpty()) return 0.000975;

            JsonNode dimension = priceDimensions.elements().next();
            return dimension.path("pricePerUnit").path("USD").asDouble(0.000975);

        } catch (Exception e) {
            log.warning("fetchDynamoDBGlobalTableWriteReplicationUnitsPrice failed: " + e.getMessage());
            return 0.000975;
        }
    }
}

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

public class AWSDatabaseCostCalculator extends AWSCloudCalculator implements DatabaseCostCalculator {

    private static final Logger log = Logger.getLogger(AWSDatabaseCostCalculator.class.getName());

    @Override
    public Map<String, Object> calculateDatabaseBackupPricing() {
        Map<String, Object> result = new LinkedHashMap<>();

        // TODO Get from AWS
        // S3 Standard storage for backups
        result.put("s3StandardPerGbMonth",   fetchSimplePrice("AmazonS3",  "S3 Standard", "Storage", 0.023));
        // RDS Snapshot storage
        result.put("rdsSnapshotPerGbMonth",  fetchSimplePrice("AmazonRDS", "RDS Snapshot", "Database Storage", 0.095));
        // RDS Multi-AZ surcharge (approx 2× Single-AZ)
        result.put("rdsMultiAzSurchargeNote","Multi-AZ roughly doubles the RDS instance cost. Select instance above to compute.");
        // Aurora replica — per replica per hour (us-east-1, db.r6g.large as reference)
        result.put("auroraReplicaPerHour",   fetchAuroraReplicaPrice());
        // DynamoDB Global Tables — replicated write cost $0.000975/WRU additional per region
        result.put("dynamoGlobalTablePerWruUsd", 0.000975);
        result.put("dynamoGlobalTableNote",  "Add ~$0.000975/WRU per extra replication region beyond the primary.");
        return result;
    }

    private double fetchAuroraReplicaPrice() {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonRDS")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("databaseEngine").value("Aurora MySQL").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value("db.r6g.large").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("deploymentOption").value("Multi-AZ").build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();
            GetProductsResponse resp = pricing.getProducts(req);
            if (resp.priceList().isEmpty()) return 0.26;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            return root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(0.26);
        } catch (Exception e) { return 0.26; }
    }
}

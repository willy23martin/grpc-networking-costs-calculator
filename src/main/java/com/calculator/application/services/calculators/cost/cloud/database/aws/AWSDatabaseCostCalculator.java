package com.calculator.application.services.calculators.cost.cloud.database.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.DatabaseCostCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AWSDatabaseCostCalculator extends AWSCloudCalculator implements DatabaseCostCalculator {

    private static final Logger log = Logger.getLogger(AWSDatabaseCostCalculator.class.getName());

    @PostConstruct
    @Override
    public Map<String, Object> calculateDatabaseBackupPricing() {
        Map<String, Object> databaseBackupCosts = new LinkedHashMap<>();

        mapS3StandardStorageForBackupsCosts(databaseBackupCosts);
        mapRDSSnapshotStorageCosts(databaseBackupCosts);
        mapRDSMultiAZSurchargeCosts(databaseBackupCosts);
        mapAuroraReplicaPerReplicaPerHourCosts(databaseBackupCosts);
        mapDynamoDBGlobalTablesReplicatedWriteCosts(databaseBackupCosts);

        return databaseBackupCosts;
    }

    // AWSDatabaseCostCalculator — current (broken for API failure test)
    private void mapS3StandardStorageForBackupsCosts(Map<String, Object> map) {
        double price = fetchSimplePrice(log, "AmazonS3", "Storage", 0.023); // throws, not caught here
        map.put("s3StandardStoragePerGbUsd", price);
        log.info("S3 Standard Storage For Backups Costs have been mapped");
    }

    private void mapRDSSnapshotStorageCosts(Map<String, Object> map) {
        double price = fetchSimplePrice(log, "AmazonRDS", "Database Storage", 0.095); // throws, not caught here
        map.put("rdsSnapshotStoragePerGbUsd", price);
    }

    private static void mapRDSMultiAZSurchargeCosts(Map<String, Object> databaseBackupCosts) {
        databaseBackupCosts.put("rdsMultiAzSurchargeNote","Multi-AZ roughly doubles the RDS instance cost. Select instance above to compute.");
        log.info("RDS MultiAZ Surcharge Costs have been mapped.");
    }

    private void mapAuroraReplicaPerReplicaPerHourCosts(Map<String, Object> databaseBackupCosts) {
        databaseBackupCosts.put("auroraReplicaPerHour",   fetchAuroraReplicaPrice());
        log.info("Aurora Replica Per Hour Costs have been mapped.");
    }

    private static void mapDynamoDBGlobalTablesReplicatedWriteCosts(Map<String, Object> databaseBackupCosts) {
        databaseBackupCosts.put("dynamoGlobalTablePerWruUsd", 0.000975);
        databaseBackupCosts.put("dynamoGlobalTableNote",  "Add ~$0.000975/WRU per extra replication region beyond the primary.");
        log.info("Dynamo DB Global Tables Replicated Write Costs have been mapped.");
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
            GetProductsResponse resp = pricingClient.getProducts(req);
            log.info("GetProductsResponse database " + resp);
            if (resp.priceList().isEmpty()) return 0.26;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            return root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(0.26);
        } catch (Exception e) { return 0.26; }
    }
}

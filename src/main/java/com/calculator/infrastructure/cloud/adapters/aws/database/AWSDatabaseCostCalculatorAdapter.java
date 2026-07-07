package com.calculator.infrastructure.cloud.adapters.aws.database;

import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.calculator.application.services.calculators.cost.cloud.ports.DatabaseCostCalculatorPort;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.pricing.PricingClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AWSDatabaseCostCalculatorAdapter extends AWSCloudCalculatorAdapter implements DatabaseCostCalculatorPort {

    private static final Logger log = Logger.getLogger(AWSDatabaseCostCalculatorAdapter.class.getName());

    private final AWSAuroraDatabaseCostCalculator auroraDatabaseCostCalculator;

    private final AWSDynamoDBCostCalculator awsDynamoDBCostCalculator;

    public AWSDatabaseCostCalculatorAdapter(PricingClient pricingClient) {
        super(pricingClient);
        auroraDatabaseCostCalculator = new AWSAuroraDatabaseCostCalculator(pricingClient);
        awsDynamoDBCostCalculator = new AWSDynamoDBCostCalculator(pricingClient);
    }

    @Override
    public Map<String, Object> calculateDatabaseBackupPricing() {
        Map<String, Object> databaseBackupCosts = new LinkedHashMap<>();

        mapS3StandardStorageForBackupsCosts(databaseBackupCosts);
        mapRDSSnapshotStorageCosts(databaseBackupCosts);
        mapRDSMultiAZSurchargeCosts(databaseBackupCosts);
        databaseBackupCosts.putAll(auroraDatabaseCostCalculator.calculateDatabaseBackupPricing());
        databaseBackupCosts.putAll(awsDynamoDBCostCalculator.calculateDatabaseBackupPricing());

        return databaseBackupCosts;
    }

    private void mapS3StandardStorageForBackupsCosts(Map<String, Object> map) {
        double price = fetchSimplePrice(log, "AmazonS3", "Storage", 0.3); // throws, not caught here
        map.put("s3StandardStoragePerGbUsd", price);
        log.info("S3 Standard Storage For Backups Costs have been mapped: \n" + map);
    }

    private void mapRDSSnapshotStorageCosts(Map<String, Object> map) {
        double price = fetchSimplePrice(log, "AmazonRDS", "Database Storage", 0.095); // throws, not caught here
        map.put("rdsSnapshotStoragePerGbUsd", price);
        log.info("rdsSnapshotStoragePerGbUsd: \n" + price);
    }

    private static void mapRDSMultiAZSurchargeCosts(Map<String, Object> databaseBackupCosts) {
        databaseBackupCosts.put("rdsMultiAzSurchargeNote","Multi-AZ roughly doubles the RDS instance cost. Select instance above to compute.");
        log.info("RDS MultiAZ Surcharge Costs have been mapped: \n" + databaseBackupCosts);
    }

    @PostConstruct
    private void init(){
        calculateDatabaseBackupPricing();
    }
}

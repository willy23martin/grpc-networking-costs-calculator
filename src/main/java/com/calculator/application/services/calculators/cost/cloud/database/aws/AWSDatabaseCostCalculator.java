package com.calculator.application.services.calculators.cost.cloud.database.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.DatabaseCostCalculator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AWSDatabaseCostCalculator extends AWSCloudCalculator implements DatabaseCostCalculator {

    private static final Logger log = Logger.getLogger(AWSDatabaseCostCalculator.class.getName());

    @Autowired
    private AWSAuroraDatabaseCostCalculator auroraDatabaseCostCalculator;

    @Autowired
    private AWSDynamoDBCostCalculator awsDynamoDBCostCalculator;

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

    @PostConstruct
    private void init(){
        calculateDatabaseBackupPricing();
    }
}

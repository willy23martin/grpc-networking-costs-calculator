package com.calculator.cost.cloud.database.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.aws.AWSDatabaseCostCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AWSDatabaseCostCalculatorTest {

    @Mock
    private PricingClient pricingMock;
    @Mock
    private ObjectMapper mapperMock;
    @Mock
    private JsonNode rootMock;

    private AWSDatabaseCostCalculator calculator;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new AWSDatabaseCostCalculator();
        injectMocks(calculator);
    }

    @Test
    void calculateDatabaseBackupPricing_apiFailures_usesFallbacks() throws Exception {
        // Inject mock
        Field pricingField = calculator.getClass().getSuperclass().getDeclaredField("pricingClient");
        pricingField.setAccessible(true);
        pricingField.set(calculator, pricingMock);

        // Every getProducts call throws
        when(pricingMock.getProducts(any(GetProductsRequest.class)))
                .thenThrow(new RuntimeException("API fail"));

        // Should NOT throw — fetchSimplePrice must absorb it and return fallbacks
        Map<String, Object> result = calculator.calculateDatabaseBackupPricing();

        assertThat(result).isNotNull();
        assertThat(result).containsKey("s3StandardStoragePerGbUsd");
        assertThat(result).containsKey("rdsSnapshotStoragePerGbUsd");
        // Values should be the fallback constants, not zero
        assertThat((double) result.get("s3StandardStoragePerGbUsd")).isGreaterThan(0);
        assertThat((double) result.get("rdsSnapshotStoragePerGbUsd")).isGreaterThan(0);
    }

    private void injectMocks(AWSDatabaseCostCalculator calc) throws Exception {
        Field pricingField = AWSCloudCalculator.class.getDeclaredField("pricingClient");
        pricingField.setAccessible(true);
        pricingField.set(calc, pricingMock);

        Field mapperField = AWSCloudCalculator.class.getSuperclass().getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(calc, mapperMock);
    }
}
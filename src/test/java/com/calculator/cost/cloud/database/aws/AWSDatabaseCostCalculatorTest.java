package com.calculator.cost.cloud.database.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.aws.AWSDatabaseCostCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
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
    void calculateDatabaseBackupPricing_apiFailures_usesFallbacks() {
        when(pricingMock.getProducts((GetProductsRequest) any())).thenThrow(new RuntimeException("API fail"));

        Map<String, Object> result = calculator.calculateDatabaseBackupPricing();

        assertEquals(0.095, result.get("rdsSnapshotPerGbMonth"));
        assertEquals(0.26, result.get("auroraReplicaPerHour"));
    }

    private void injectMocks(AWSDatabaseCostCalculator calc) throws Exception {
        Field pricingField = AWSCloudCalculator.class.getDeclaredField("pricing");
        pricingField.setAccessible(true);
        pricingField.set(calc, pricingMock);

        Field mapperField = AWSCloudCalculator.class.getSuperclass().getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(calc, mapperMock);
    }
}
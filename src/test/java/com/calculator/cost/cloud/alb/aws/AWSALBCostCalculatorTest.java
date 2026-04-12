package com.calculator.cost.cloud.alb.aws;

import com.calculator.application.services.calculators.cost.cloud.alb.aws.alb.AWSALBCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
class AWSALBCostCalculatorTest {

    @Mock private PricingClient pricingMock;
    @Mock private ObjectMapper mapperMock;
    @Mock private JsonNode rootMock;

    private AWSALBCostCalculator calculator;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new AWSALBCostCalculator();
        injectMocks(calculator);
    }

    @Test
    void calculateALBCosts_apiEmptyList_usesAWSPricingAPI() {
        when(pricingMock.getProducts((GetProductsRequest) any()))
                .thenReturn(GetProductsResponse.builder().priceList(List.of()).build());

        Map<String, Object> result = calculator.calculateALBCosts();

        assertEquals(0.008, result.get("fixedPerHourUsd"));
        assertEquals(0.008, result.get("lcuPerHourUsd"));
        assertEquals(5.84, result.get("fixedPerMonthUsd"));
        assertEquals(5.84, result.get("lcuPerMonthBase"));
        assertEquals("AWS Pricing API", result.get("source"));
    }

    @Test
    void calculateALBCosts_apiException_fullFallback() {
        when(pricingMock.getProducts((GetProductsRequest) any())).thenThrow(new RuntimeException("API timeout"));

        Map<String, Object> result = calculator.calculateALBCosts();

        assertEquals(0.008, result.get("fixedPerHourUsd"));
        assertEquals(5.84, result.get("fixedPerMonthUsd"));
        assertEquals("fallback", result.get("source"));
    }

    @Test
    void calculateALBCosts_monthlyRounding_correctMath() {
        when(pricingMock.getProducts((GetProductsRequest) any()))
                .thenReturn(GetProductsResponse.builder().priceList(List.of("")).build());

        Map<String, Object> result = calculator.calculateALBCosts();

        double expectedMonthly = Math.round(0.008 * 730 * 100.0) / 100.0;
        assertEquals(expectedMonthly, result.get("fixedPerMonthUsd"));
        assertEquals(5.84, result.get("fixedPerMonthUsd"));  // Verified calc
    }

    private void injectMocks(AWSALBCostCalculator awsalbCostCalculator) throws Exception {
        Field pricingField = AWSCloudCalculator.class.getDeclaredField("pricing");
        pricingField.setAccessible(true);
        pricingField.set(awsalbCostCalculator, pricingMock);

        Field mapperField = AWSCloudCalculator.class.getSuperclass().getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(awsalbCostCalculator, mapperMock);
    }
}
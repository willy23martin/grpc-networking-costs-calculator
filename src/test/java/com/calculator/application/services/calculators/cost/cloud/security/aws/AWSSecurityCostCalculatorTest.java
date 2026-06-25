package com.calculator.application.services.calculators.cost.cloud.security.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSSecurityCostCalculatorTest {

    @Mock
    private PricingClient pricingClient;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private AWSSecurityCostCalculator securityCostCalculator;

    private static final String MOCK_PRICING_JSON = "{"
            + "  \"terms\": {"
            + "    \"OnDemand\": {"
            + "      \"RANDOM_HASH_A\": {"
            + "        \"priceDimensions\": {"
            + "          \"RANDOM_HASH_B\": {"
            + "            \"pricePerUnit\": {\"USD\": \"3.75\"}"
            + "          }"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityCostCalculator, "mapper", mapper);
    }

    @Test
    @DisplayName("Should populate all values dynamically from AWS Price List API responses")
    void shouldReturnLiveApiPricesWhenAvailable() {
        GetProductsResponse mockResponse = GetProductsResponse.builder()
                .priceList(Collections.singletonList(MOCK_PRICING_JSON))
                .build();

        when(pricingClient.getProducts(any(GetProductsRequest.class))).thenReturn(mockResponse);

        Map<String, Object> results = securityCostCalculator.calculateSecurityCosts();

        assertNotNull(results);
        assertEquals(3.75, results.get("guardDutyPerGbLogs"));
        assertEquals(3.75, results.get("inspectorPerInstanceMonth"));
        assertEquals(3.75, results.get("wafWebAclPerMonth"));
        assertEquals(3.75, results.get("kmsCmkPerMonth"));
        assertEquals(3.75, results.get("cloudwatchLogsIngestionPerGb"));
    }

    @Test
    @DisplayName("Should gracefully fall back to static baselines on API empty sets or failures")
    void shouldGracefullyFallbackOnApiFailures() {
        GetProductsResponse emptyResponse = GetProductsResponse.builder()
                .priceList(Collections.emptyList())
                .build();

        when(pricingClient.getProducts(any(GetProductsRequest.class))).thenReturn(emptyResponse);

        Map<String, Object> results = securityCostCalculator.calculateSecurityCosts();

        assertNotNull(results);
        assertEquals(1.00, results.get("guardDutyPerGbLogs"));
        assertEquals(1.178, results.get("inspectorPerInstanceMonth"));
        assertEquals(5.00, results.get("wafWebAclPerMonth"));
        assertEquals(1.00, results.get("kmsCmkPerMonth"));
        assertEquals(0.03, results.get("kmsApiCallsPer10k"));
        assertEquals(0.50, results.get("cloudwatchLogsIngestionPerGb"));
        assertEquals(0.03, results.get("cloudwatchLogsStoragePerGbMonth"));
    }
}
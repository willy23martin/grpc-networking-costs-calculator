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
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
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
            + "  \"product\": {"
            + "    \"attributes\": {"
            + "      \"usagetype\": \"customer managed key kms apikey resource assessment putlogevents archivestorage ec2 analysis\","
            + "      \"description\": \"mock tracking data discovery criteria matching text requirement\""
            + "    }"
            + "  },"
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
    @DisplayName("Should successfully capture and traverse live JSON parameters via modern case-insensitive evaluation")
    void shouldReturnLiveApiPricesWhenAvailable() {
        GetProductsResponse mockResponse = GetProductsResponse.builder()
                .priceList(Collections.singletonList(MOCK_PRICING_JSON))
                .build();

        when(pricingClient.getProducts(any(GetProductsRequest.class))).thenReturn(mockResponse);

        Map<String, Object> results = securityCostCalculator.calculateSecurityCosts();

        assertNotNull(results);
        assertEquals(3.75, results.get("guardDutyPerGbLogs"));
        assertEquals(3.75, results.get("inspectorPerInstanceMonth"));
        assertEquals(3.75, results.get("maciePerGbClassified"));
        assertEquals(3.75, results.get("cloudwatchLogsIngestionPerGb"));
        assertEquals(3.75, results.get("cloudwatchLogsStoragePerGbMonth"));
        assertEquals(3.75, results.get("auditManagerPerAssessmentMonth"));
        assertEquals(3.75, results.get("kmsCmkPerMonth"));
        assertEquals(3.75, results.get("kmsApiCallsPer10k"));
        assertEquals(3.75, results.get("wafWebAclPerMonth"));
        assertEquals(3.75, results.get("wafRulePerMonth"));
        assertEquals(3.75, results.get("wafPer1MRequests"));
    }

    @Test
    @DisplayName("Should return accurate hardcoded defaults when live pricing responses return empty lists")
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
        assertEquals(1.00, results.get("wafRulePerMonth"));
        assertEquals(0.60, results.get("wafPer1MRequests"));
    }

    @Test
    @DisplayName("Should swiftly recover and use baseline fallbacks when the AWS SDK throws an ApiCallTimeoutException")
    void shouldFallbackGracefullyOnApiTimeout() {
        when(pricingClient.getProducts(any(GetProductsRequest.class)))
                .thenThrow(ApiCallTimeoutException.create("API call exceeded configured total timeout threshold", null));

        Map<String, Object> results = securityCostCalculator.calculateSecurityCosts();

        assertNotNull(results);
        assertEquals(1.00, results.get("guardDutyPerGbLogs"));
        assertEquals(5.00, results.get("wafWebAclPerMonth"));
    }
}
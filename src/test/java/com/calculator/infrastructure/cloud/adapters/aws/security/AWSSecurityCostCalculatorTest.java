package com.calculator.infrastructure.cloud.adapters.aws.security;

import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.Collections;
import java.util.Map;

import static com.calculator.infrastructure.cloud.adapters.aws.security.AWSSecurityCostCalculatorAdapter.SECURITY_CLOUD_SERVICE_AMAZON_GUARDUTY_FALLBACK_VALUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSSecurityCostCalculatorTest {

    @Mock
    private PricingClient pricingClient;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private AWSSecurityWAFCostCalculator awsSecurityWAFCostCalculator;

    @Mock
    private SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @InjectMocks
    private CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    @InjectMocks
    private AWSSecurityCostCalculatorAdapter securityCostCalculator;

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
        Mockito.lenient().when(securityArchitecturalDecisionRepository.getOAuthTactic())
                .thenReturn(ArchitecturalTactic.builder().id("tactic-oauth").name("OAuth 2.0 + JWT").build());
        Mockito.lenient().when(securityArchitecturalDecisionRepository.getMTLSTactic())
                .thenReturn(ArchitecturalTactic.builder().id("tactic-mtls").name("mTLS").build());
        Mockito.lenient().when(securityArchitecturalDecisionRepository.getTLSTactic())
                .thenReturn(ArchitecturalTactic.builder().id("tactic-tls").name("TLS").build());

        ReflectionTestUtils.setField(securityCostCalculator, "mapper", mapper);
        ReflectionTestUtils.setField(securityCostCalculator, "cloudSecurityArchitecturalDecisionRepository", cloudSecurityArchitecturalDecisionRepository);
    }

    @Test
    @DisplayName("Should successfully capture and traverse live JSON parameters via modern case-insensitive evaluation")
    void shouldReturnLiveApiPricesWhenAvailable() {
        final GetProductsResponse mockResponse = GetProductsResponse.builder()
                .priceList(Collections.singletonList(MOCK_PRICING_JSON))
                .build();

        when(awsSecurityWAFCostCalculator.calculateSecurityCosts()).thenReturn(
                Map.ofEntries(
                        Map.entry("wafWebAclPerMonth", 3.75),
                        Map.entry("wafRulePerMonth", 3.75),
                        Map.entry("wafPer1MRequests", 3.75)
                )
        );
        when(pricingClient.getProducts(any(GetProductsRequest.class))).thenReturn(mockResponse);

        final Map<String, Object> results = securityCostCalculator.calculateSecurityCosts();

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
        final GetProductsResponse emptyResponse = GetProductsResponse.builder()
                .priceList(Collections.emptyList())
                .build();

        when(awsSecurityWAFCostCalculator.calculateSecurityCosts()).thenReturn(
                Map.ofEntries(
                        Map.entry("wafWebAclPerMonth", 5.00),
                        Map.entry("wafRulePerMonth", 1.00),
                        Map.entry("wafPer1MRequests", 0.60)
                )
        );
        when(pricingClient.getProducts(any(GetProductsRequest.class))).thenReturn(emptyResponse);

        final Map<String, Object> results = securityCostCalculator.calculateSecurityCosts();

        assertNotNull(results);
        assertEquals(1.00, results.get("guardDutyPerGbLogs"));
        assertEquals(1.2528, results.get("inspectorPerInstanceMonth"));
        assertEquals(5.00, results.get("wafWebAclPerMonth"));
        assertEquals(1.00, results.get("wafRulePerMonth"));
        assertEquals(0.60, results.get("wafPer1MRequests"));
    }

    @Test
    @DisplayName("Should swiftly recover and use baseline fallbacks when the AWS SDK throws an ApiCallTimeoutException")
    void shouldFallbackGracefullyOnApiTimeout() {
        when(awsSecurityWAFCostCalculator.calculateSecurityCosts()).thenReturn(
                Map.ofEntries(
                        Map.entry("wafWebAclPerMonth", 5.00),
                        Map.entry("wafRulePerMonth", 1.00),
                        Map.entry("wafPer1MRequests", 0.60)
                )
        );

        when(pricingClient.getProducts(any(GetProductsRequest.class)))
                .thenThrow(ApiCallTimeoutException.create("API call exceeded configured total timeout threshold", null));

        final Map<String, Object> results = securityCostCalculator.calculateSecurityCosts();

        assertNotNull(results);
        assertEquals(1.00, results.get("guardDutyPerGbLogs"));
        assertEquals(5.00, results.get("wafWebAclPerMonth"));
    }

}
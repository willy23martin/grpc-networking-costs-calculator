package com.calculator.infrastructure.web.rest;

import com.calculator.domain.repository.cloud.reliability.CloudReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.resiliency.CloudResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ContainerizedEnvironmentCostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PricingClient pricingClientMock;

    // Architecture decision repositories to satisfy application context initialization
    @MockitoBean
    private SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;
    @MockitoBean
    private CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;
    @MockitoBean
    private ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;
    @MockitoBean
    private CloudReliabilityArchitecturalDecisionRepository cloudReliabilityArchitecturalDecisionRepository;
    @MockitoBean
    private ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository;
    @MockitoBean
    private CloudResiliencyArchitecturalDecisionRepository cloudResiliencyArchitecturalDecisionRepository;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(reliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudReliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(resiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudResiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());
    }

    @Test
    void getContainerPricing_successfulExecution() throws Exception {
        // Mock payload for the 1st invocation (vCPU Price)
        String mockVcpuJson = """
                {
                  "terms": {
                    "OnDemand": {
                      "term1": {
                        "priceDimensions": {
                          "dim1": {
                            "pricePerUnit": { "USD": "0.04048" }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        // Mock payload for the 2nd invocation (GB RAM Price)
        String mockGbJson = """
                {
                  "terms": {
                    "OnDemand": {
                      "term2": {
                        "priceDimensions": {
                          "dim2": {
                            "pricePerUnit": { "USD": "0.004445" }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        // Mock payload for any subsequent sequential invocations (e.g. EC2 instances pricing loop)
        String mockFallbackJson = "{\"terms\": {\"OnDemand\": {}}}";

        GetProductsResponse vcpuResponse = GetProductsResponse.builder().priceList(List.of(mockVcpuJson)).build();
        GetProductsResponse gbResponse = GetProductsResponse.builder().priceList(List.of(mockGbJson)).build();
        GetProductsResponse fallbackResponse = GetProductsResponse.builder().priceList(List.of(mockFallbackJson)).build();

        // Chain multiple return responses sequentially to cover the whole lookup sequence cleanly
        when(pricingClientMock.getProducts(any(GetProductsRequest.class)))
                .thenReturn(vcpuResponse)
                .thenReturn(gbResponse)
                .thenReturn(fallbackResponse);

        mockMvc.perform(get("/api/aws/container-pricing")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fargateVcpuPerHour").value(0.04048))
                .andExpect(jsonPath("$.fargateGbPerHour").value(0.004445))
                .andExpect(jsonPath("$.source").value("AWS Pricing API (live) + fallback for unavailable types"));
    }

    @Test
    void extractOnDemandPriceFromJson_missingPriceDimensions_returnsFallbackDefaultValues() throws Exception {
        // Passing structurally partial/faulty JSON structures directly down the code paths
        String faultyJson = "{\"terms\": {\"OnDemand\": {}}}";

        GetProductsResponse faultyResponse = GetProductsResponse.builder()
                .priceList(List.of(faultyJson))
                .build();

        // Let all internal pricing extraction passes hit the JSON parsing exception block
        when(pricingClientMock.getProducts(any(GetProductsRequest.class)))
                .thenReturn(faultyResponse);

        mockMvc.perform(get("/api/aws/container-pricing"))
                .andExpect(status().isOk())
                // Validates that it drops the parsing anomalies and retains hardcoded production fallback safety boundaries
                .andExpect(jsonPath("$.fargateVcpuPerHour").value(0.04048))
                .andExpect(jsonPath("$.fargateGbPerHour").value(0.004445));
    }
}
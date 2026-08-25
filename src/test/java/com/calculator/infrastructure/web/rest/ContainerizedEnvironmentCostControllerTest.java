package com.calculator.infrastructure.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestRepositoryStubsConfiguration.class)
class ContainerizedEnvironmentCostControllerTest extends BaseIntegrationTest {

    @MockitoBean
    private PricingClient pricingClientMock;

    @Test
    void getContainerPricing_successfulExecution() throws Exception {
        final String mockVcpuJson = """
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
        final String mockGbJson = """
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

        final String mockFallbackJson = "{\"terms\": {\"OnDemand\": {}}}";

        final GetProductsResponse vcpuResponse = GetProductsResponse.builder().priceList(List.of(mockVcpuJson)).build();
        final GetProductsResponse gbResponse = GetProductsResponse.builder().priceList(List.of(mockGbJson)).build();
        final GetProductsResponse fallbackResponse = GetProductsResponse.builder().priceList(List.of(mockFallbackJson)).build();

        when(pricingClientMock.getProducts(any(GetProductsRequest.class)))
                .thenReturn(vcpuResponse)
                .thenReturn(gbResponse)
                .thenReturn(fallbackResponse);

        mockMvc.perform(get("/api/cloud/container-pricing")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fargateVcpuPerHour").value(0.04048))
                .andExpect(jsonPath("$.fargateGbPerHour").value(0.004445))
                .andExpect(jsonPath("$.source").value("AWS Pricing API (live) + fallback for unavailable types"));
    }

    @Test
    void extractOnDemandPriceFromJson_missingPriceDimensions_returnsFallbackDefaultValues() throws Exception {
        final String faultyJson = "{\"terms\": {\"OnDemand\": {}}}";

        final GetProductsResponse faultyResponse = GetProductsResponse.builder()
                .priceList(List.of(faultyJson))
                .build();

        when(pricingClientMock.getProducts(any(GetProductsRequest.class)))
                .thenReturn(faultyResponse);

        mockMvc.perform(get("/api/cloud/container-pricing"))
                .andExpect(status().isOk())
                // Validates that it drops the parsing anomalies and retains hardcoded production fallback safety boundaries
                .andExpect(jsonPath("$.fargateVcpuPerHour").value(0.04048))
                .andExpect(jsonPath("$.fargateGbPerHour").value(0.004445));
    }
}
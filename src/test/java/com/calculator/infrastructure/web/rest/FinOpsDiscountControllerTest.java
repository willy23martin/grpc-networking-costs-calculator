package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.PricingClientBuilder;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinOpsDiscountControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Coverage: Live API Success Path")
    void testLiveApiSuccess() throws Exception {
        try (MockedStatic<PricingClient> mockedPricing = Mockito.mockStatic(PricingClient.class)) {
            PricingClient mockClient = mock(PricingClient.class);
            PricingClientBuilder mockBuilder = mock(PricingClientBuilder.class);
            mockedPricing.when(PricingClient::builder).thenReturn(mockBuilder);
            when(mockBuilder.region(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);

            String validJson = "{"
                    + "\"terms\": {"
                    + "  \"OnDemand\": { \"key1\": { \"priceDimensions\": { \"dim1\": { \"pricePerUnit\": { \"USD\": \"0.20\" } } } } },"
                    + "  \"Reserved\": { \"key2\": { \"priceDimensions\": { \"dim2\": { \"pricePerUnit\": { \"USD\": \"0.10\" } } } } }"
                    + " }"
                    + "}";

            GetProductsResponse resp = GetProductsResponse.builder().priceList(List.of(validJson)).build();
            when(mockClient.getProducts(any(GetProductsRequest.class))).thenReturn(resp);

            mockMvc.perform(get("/api/finops/ri-prices/m6i.large"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.source", containsString("Live")))
                    .andExpect(jsonPath("$.onDemandPerHourUsd", is(0.2)));
        }
    }

    @Test
    @DisplayName("Coverage: Malformed JSON & Missing Nodes")
    void testJsonEdgeCases() throws Exception {
        try (MockedStatic<PricingClient> mockedPricing = Mockito.mockStatic(PricingClient.class)) {
            PricingClient mockClient = mock(PricingClient.class);
            PricingClientBuilder mockBuilder = mock(PricingClientBuilder.class);
            mockedPricing.when(PricingClient::builder).thenReturn(mockBuilder);
            when(mockBuilder.region(any())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);

            String noDims = "{\"terms\":{\"OnDemand\":{\"k\":{\"priceDimensions\":{}}}}}";

            GetProductsResponse resp = GetProductsResponse.builder().priceList(List.of(noDims)).build();
            when(mockClient.getProducts(any(GetProductsRequest.class))).thenReturn(resp);

            mockMvc.perform(get("/api/finops/ri-prices/t3.medium"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.source", containsString("Fallback")));
        }
    }

    @Test
    @DisplayName("Coverage: AWS Client Exception Catch")
    void testClientException() throws Exception {
        try (MockedStatic<PricingClient> mockedPricing = Mockito.mockStatic(PricingClient.class)) {
            mockedPricing.when(PricingClient::builder).thenThrow(new RuntimeException("API Down"));

            mockMvc.perform(get("/api/finops/ri-prices/m6i.large"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.source", containsString("Fallback")));
        }
    }
}
package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.PricingClientBuilder;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.List;

import static com.calculator.application.services.utils.MathUtils.round2;
import static com.calculator.application.services.utils.MathUtils.round4;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FinOpsDiscountControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("Coverage: Discount Matrix Calculations")
    void testDiscountCalculationPath() throws Exception {
        FinOpsDiscountController.DiscountRequest req = new FinOpsDiscountController.DiscountRequest();
        req.currentMonthlyContainerCostUsd = 1000.0;
        req.riStandard1yr = true;
        req.riStandard3yr = true;

        mockMvc.perform(post("/api/finops/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestStrategy", is("Standard RI 3-Yr")))
                .andExpect(jsonPath("$.maxDiscountPct", is(57.0)))
                .andExpect(jsonPath("$.calculatedMonthlySavingUsd", is(570.0)))
                .andExpect(jsonPath("$.netMonthlyContainerCostUsd", is(430.0)));
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

    @Test
    @DisplayName("Coverage: No Strategy Selected")
    void testEmptyOptions() throws Exception {
        FinOpsDiscountController.DiscountRequest req = new FinOpsDiscountController.DiscountRequest();
        req.currentMonthlyContainerCostUsd = 500.0;
        req.riStandard1yr = false;
        req.riStandard3yr = false;
        req.riConvertible1yr = false;
        req.savingsPlan1yr = false;
        req.savingsPlan3yr = false;

        mockMvc.perform(post("/api/finops/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestStrategy", nullValue()))
                .andExpect(jsonPath("$.maxDiscountPct", is(0.0)))
                .andExpect(jsonPath("$.netMonthlyContainerCostUsd", is(500.0)));
    }

    @Test
    @DisplayName("Coverage: Math Utils")
    void testMath() {
        assert(round2(5.555) == 5.56);
        assert(round4(0.12344) == 0.1234);
    }
}
package com.calculator.infrastructure.cloud.adapters.aws.compute;

import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AWSComputeCostCalculatorTest {

    @Mock
    private PricingClient pricingMock;
    @Mock
    private ObjectMapper mapperMock;

    private AWSComputeCostCalculatorAdapter calculator;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new AWSComputeCostCalculatorAdapter(pricingMock);
        injectMocks(calculator);
    }

    @Test
    void calculatePriceByComputeInstance_allApiSuccess_returns16Entries() {
        when(pricingMock.getProducts((GetProductsRequest) any()))
                .thenReturn(GetProductsResponse.builder().priceList(List.of("success")).build())
                .thenReturn(GetProductsResponse.builder().priceList(List.of("success")).build())
                .thenReturn(GetProductsResponse.builder().priceList(List.of("success")).build());

        List<Map<String, Object>> result = calculator.calculatePriceByComputeInstance();

        assertEquals(18, result.size());
        assertEquals("t3.micro", result.getFirst().get("instanceType"));
        assertEquals(62500000L, result.getFirst().get("networkBytesPerSec"));  // 0.5Gbps math
        verify(pricingMock, times(18)).getProducts((GetProductsRequest) any());  // Full loop coverage
    }

    @Test
    void calculatePriceByComputeInstance_apiExceptions_usesFallback() {
        when(pricingMock.getProducts((GetProductsRequest) any())).thenThrow(new RuntimeException("API fail"));

        List<Map<String, Object>> result = calculator.calculatePriceByComputeInstance();

        assertEquals(18, result.size());
        assertEquals(0.0104, result.get(0).get("pricePerHourUsd"));  // t3.micro fallback
        assertEquals(0.0208, result.get(1).get("pricePerHourUsd"));  // t3.small fallback
        assertEquals(0.10, findEntry(result, "m6i.8xlarge").get("pricePerHourUsd"));  // Default fallback
    }

    @Test
    void calculatePriceByComputeInstance_emptyResponses_usesFallbackZeroPath() {
        when(pricingMock.getProducts((GetProductsRequest) any()))
                .thenReturn(GetProductsResponse.builder().priceList(List.of()).build());

        List<Map<String, Object>> result = calculator.calculatePriceByComputeInstance();

        assertEquals(18, result.size());
        assertEquals(0.0, result.get(2).get("pricePerHourUsd"));
    }

    @Test
    void calculatePriceByComputeInstance_networkBandwidth_allValues() {
        when(pricingMock.getProducts((GetProductsRequest) any())).thenThrow(new RuntimeException("fail"));

        List<Map<String, Object>> result = calculator.calculatePriceByComputeInstance();

        assertEquals(0.5, result.getFirst().get("networkGbps"));              // for t3 family
        assertEquals(12.5, findEntry(result, "m6i.large").get("networkGbps"));
        assertEquals(25.0, findEntry(result, "m6i.4xlarge").get("networkGbps"));
        assertEquals(3125000000L, findEntry(result, "m6i.4xlarge").get("networkBytesPerSec"));
    }

    private void injectMocks(AWSComputeCostCalculatorAdapter calc) throws Exception {
        Field pricingField = AWSCloudCalculatorAdapter.class.getDeclaredField("pricingClient");
        pricingField.setAccessible(true);
        pricingField.set(calc, pricingMock);

        Field mapperField = AWSCloudCalculatorAdapter.class.getSuperclass().getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(calc, mapperMock);
    }

    private Map<String, Object> findEntry(List<Map<String, Object>> result, String instanceType) {
        return result.stream()
                .filter(e -> instanceType.equals(e.get("instanceType")))
                .findFirst().orElseThrow();
    }
}

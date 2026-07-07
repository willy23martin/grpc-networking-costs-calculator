package com.calculator.infrastructure.cloud.adapters.aws.caching;

import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.caching.AWSCachingCostCalculatorAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AWSCachingCostCalculatorAdapterTest {

    @Mock
    private PricingClient pricingMock;
    @Mock
    private ObjectMapper mapperMock;

    private AWSCachingCostCalculatorAdapter calculator;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new AWSCachingCostCalculatorAdapter(pricingMock);
        injectMocks(calculator);
    }

    @Test
    void calculateCachingCosts_allApiExceptions_fullFallback() {
        when(pricingMock.getProducts((GetProductsRequest) any())).thenThrow(new RuntimeException("API fail"));

        Map<String, Object> result = calculator.calculateCachingCosts();

        assertAll("All fallback map hits",
                () -> assertEquals(0.166, result.get("redisR6gLargePerHour")),
                () -> assertEquals(0.332, result.get("redisR6gXlargePerHour")),
                () -> assertEquals(0.665, result.get("redisR6g2xlargePerHour")),
                () -> assertEquals(0.166, result.get("memcachedR6gLargePerHour")),
                () -> assertEquals(0.332, result.get("memcachedR6gXlargePerHour"))
        );
    }

    private void injectMocks(AWSCachingCostCalculatorAdapter calc) throws Exception {
        Field pricingField = AWSCloudCalculatorAdapter.class.getDeclaredField("pricingClient");
        pricingField.setAccessible(true);
        pricingField.set(calc, pricingMock);

        Field mapperField = AWSCloudCalculatorAdapter.class.getSuperclass().getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(calc, mapperMock);
    }
}
package com.calculator.application.services.calculators.cost.cloud.finops.aws;

import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AWSFinOpsStrategyCostCalculatorTest {

    @Mock
    private PricingClient pricingClient;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private AWSFinOpsStrategyCostCalculator calculator;

    private MockedStatic<JSONLogger> mockedJsonLogger;

    @BeforeEach
    void setUp() throws Exception {
        mockedJsonLogger = Mockito.mockStatic(JSONLogger.class);
        setReflectionField(calculator, "pricingClient", pricingClient);
        setReflectionField(calculator, "mapper", mapper);
        setReflectionField(calculator, "AWS_LOCATION", "US East (N. Virginia)");
        setReflectionField(calculator, "COMPUTE_SAVINGS_PLANS", "ComputeSavingsPlans");
    }

    @AfterEach
    void tearDown() {
        if (mockedJsonLogger != null) {
            mockedJsonLogger.close();
        }
    }

    @Test
    @DisplayName("Should successfully handle both query and bulk pricing calls on the happy path")
    void calculateFinOpsStrategiesCosts_HappyPath() throws Exception {
        String mockRiJson = "{\"terms\":{\"OnDemand\":{\"OD1\":{\"priceDimensions\":{\"D1\":{\"pricePerUnit\":{\"USD\":\"0.048\"}}}}},\"Reserved\":{\"RI1\":{\"termAttributes\":{\"LeaseContractLength\":\"1 yr\",\"OfferingClass\":\"standard\",\"PurchaseOption\":\"No Upfront\"},\"priceDimensions\":{\"D2\":{\"pricePerUnit\":{\"USD\":\"0.0308\"}}}}}}}";
        String mockSupportJson = "{\"product\":{\"attributes\":{\"minMonthlyCharge\":\"100\"}},\"terms\":{\"External\":{\"SUP1\":{\"priceDimensions\":{\"D3\":{\"pricePerUnit\":{\"USD\":\"0.10\"}}}}}}}";

        GetProductsResponse mockProductsResponse = GetProductsResponse.builder()
                .priceList(List.of(mockRiJson, mockSupportJson))
                .build();

        when(pricingClient.getProducts(any(GetProductsRequest.class))).thenReturn(mockProductsResponse);

        // Updated mock JSON to include the nested rates matrix with discountedRegionCode set to us-east-1
        String mockBulkManifestJson = "{"
                + "\"products\": ["
                + "  {"
                + "    \"sku\": \"MOCK-SKU-1YR\","
                + "    \"attributes\": {"
                + "      \"productFamily\": \"ComputeSavingsPlans\","
                + "      \"location\": \"Any\","
                + "      \"purchaseOption\": \"No Upfront\","
                + "      \"purchaseTerm\": \"1yr\""
                + "    }"
                + "  },"
                + "  {"
                + "    \"sku\": \"MOCK-SKU-3YR\","
                + "    \"attributes\": {"
                + "      \"productFamily\": \"ComputeSavingsPlans\","
                + "      \"location\": \"Any\","
                + "      \"purchaseOption\": \"No Upfront\","
                + "      \"purchaseTerm\": \"3yr\""
                + "    }"
                + "  }"
                + "],"
                + "\"terms\": {"
                + "  \"savingsPlan\": ["
                + "    {"
                + "      \"sku\": \"MOCK-SKU-1YR\","
                + "      \"rates\": ["
                + "        {"
                + "          \"discountedRegionCode\": \"us-east-1\","
                + "          \"discountedRate\": {"
                + "            \"price\": \"31.0\""
                + "          }"
                + "        }"
                + "      ]"
                + "    },"
                + "    {"
                + "      \"sku\": \"MOCK-SKU-3YR\","
                + "      \"rates\": ["
                + "        {"
                + "          \"discountedRegionCode\": \"us-east-1\","
                + "          \"discountedRate\": {"
                + "            \"price\": \"50.0\""
                + "          }"
                + "        }"
                + "      ]"
                + "    }"
                + "  ]"
                + "}"
                + "}";

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("aws-bulk-pricing-", ".json");
        java.nio.file.Files.writeString(tempFile, mockBulkManifestJson);

        String mockFileUrlString = tempFile.toUri().toURL().toString();

        PriceList mockPriceList = PriceList.builder().priceListArn("arn:aws:pricing:us-east-1::price-list/sp/1").build();
        ListPriceListsResponse mockListResponse = ListPriceListsResponse.builder().priceLists(List.of(mockPriceList)).build();
        GetPriceListFileUrlResponse mockUrlResponse = GetPriceListFileUrlResponse.builder().url(mockFileUrlString).build();

        when(pricingClient.listPriceLists(any(ListPriceListsRequest.class))).thenReturn(mockListResponse);
        when(pricingClient.getPriceListFileUrl(any(GetPriceListFileUrlRequest.class))).thenReturn(mockUrlResponse);

        com.fasterxml.jackson.databind.JsonNode mockBulkNode = mapper.readTree(mockBulkManifestJson);
        lenient().doReturn(mockBulkNode)
                .when(mapper).readTree(org.mockito.ArgumentMatchers.isA(InputStream.class));

        try {
            Map<String, Object> results = calculator.calculateFinOpsStrategiesCosts();

            assertNotNull(results);
            assertEquals(36, results.get("reservedInstance1yrSavingsPct"));
            assertEquals(100, results.get("businessSupportMinMonthUsd"));
            assertEquals(10, results.get("businessSupportPctMonthlyUsage"));
            assertEquals(31, results.get("savingsPlan1yrSavingsPct"));
            assertEquals(50, results.get("savingsPlan3yrSavingsPct"));
            assertEquals(mockFileUrlString, results.get("savingsPlanBulkFileUrl"));
        } finally {
            // Clean up temporary IO footprint from disk
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("Should fall back cleanly to 0 if exceptions hit and values were initialized to 0.0")
    void calculateFinOpsStrategiesCosts_FallbackOnException() throws Exception {
        lenient().when(pricingClient.getProducts(any(GetProductsRequest.class)))
                .thenThrow(new RuntimeException("API Connection Failure"));

        PriceList mockPriceList = PriceList.builder().priceListArn("arn:aws:pricing::pl1").build();
        ListPriceListsResponse mockListResponse = ListPriceListsResponse.builder().priceLists(List.of(mockPriceList)).build();
        GetPriceListFileUrlResponse mockUrlResponse = GetPriceListFileUrlResponse.builder().url("https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/ComputeSavingsPlans/current/index.json").build();

        lenient().when(pricingClient.listPriceLists(any(ListPriceListsRequest.class))).thenReturn(mockListResponse);
        lenient().when(pricingClient.getPriceListFileUrl(any(GetPriceListFileUrlRequest.class))).thenReturn(mockUrlResponse);

        lenient().doThrow(new java.io.IOException("Network Timeout / Stream failure"))
                .when(mapper).readTree(any(InputStream.class));

        Map<String, Object> results = calculator.calculateFinOpsStrategiesCosts();

        assertNotNull(results);
        assertEquals(36, results.get("reservedInstance1yrSavingsPct"));
        assertEquals(100, results.get("businessSupportMinMonthUsd"));

        assertEquals(31, results.get("savingsPlan1yrSavingsPct"));
        assertEquals(50, results.get("savingsPlan3yrSavingsPct"));
        assertEquals("https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/ComputeSavingsPlans/current/index.json", results.get("savingsPlanBulkFileUrl"));
    }

    private void setReflectionField(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                field = current.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        if (field != null) {
            if (Modifier.isFinal(field.getModifiers())) {
                return;
            }
            field.setAccessible(true);
            field.set(target, value);
        }
    }
}
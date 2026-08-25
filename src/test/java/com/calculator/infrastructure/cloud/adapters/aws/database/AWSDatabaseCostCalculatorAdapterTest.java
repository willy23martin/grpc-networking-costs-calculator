package com.calculator.infrastructure.cloud.adapters.aws.database;

import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AWSDatabaseCostCalculatorAdapterTest {

    @Mock
    private PricingClient pricingMock;
    @Mock
    private ObjectMapper mapperMock;

    @Mock
    private AWSAuroraDatabaseCostCalculator auroraDatabaseCostCalculatorMock;

    @Mock
    private AWSDynamoDBCostCalculator awsDynamoDBCostCalculatorMock;

    private AWSDatabaseCostCalculatorAdapter calculator;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new AWSDatabaseCostCalculatorAdapter(pricingMock);
        injectMocks(calculator);
    }

    @Test
    void calculateDatabaseBackupPricing_successfulApiResponse_returnsDynamicPrices() throws Exception {
       final GetProductsResponse mockResponse = GetProductsResponse.builder()
                .priceList(List.of("{\"mock\":\"json\"}"))
                .build();

       when(pricingMock.getProducts(any(GetProductsRequest.class))).thenReturn(mockResponse);

       final JsonNode s3Root = mock(JsonNode.class);
       final JsonNode rdsRoot = mock(JsonNode.class);
       when(mapperMock.readTree(any(String.class))).thenReturn(s3Root, rdsRoot);

       setupElementsStubbing(s3Root, 0.023);
       setupElementsStubbing(rdsRoot, 0.095);

       when(auroraDatabaseCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of("auroraReplicaPerHour", 0.26));

       final String expectedNote = String.format(java.util.Locale.US, "Add ~$%.6f/WRU per extra replication region beyond the primary.", 0.000975);
       when(awsDynamoDBCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of(
                        "dynamoGlobalTablePerWruUsd", 0.000975,
                        "dynamoGlobalTableNote", expectedNote
                ));

       final Map<String, Object> result = calculator.calculateDatabaseBackupPricing();

       assertThat(result).isNotNull();
       assertEquals(0.023, result.get("s3StandardStoragePerGbUsd"));
       assertEquals(0.095, result.get("rdsSnapshotStoragePerGbUsd"));
       assertEquals(0.26, result.get("auroraReplicaPerHour"));
       assertEquals(0.000975, result.get("dynamoGlobalTablePerWruUsd"));
       assertThat((String) result.get("dynamoGlobalTableNote")).contains("0.000975");
    }

    @Test
    void calculateDatabaseBackupPricing_emptyPriceList_usesFallbacks() throws Exception {

        final GetProductsResponse emptyResponse = GetProductsResponse.builder()
                .priceList(Collections.emptyList())
                .build();

        when(pricingMock.getProducts(any(GetProductsRequest.class))).thenReturn(emptyResponse);

        when(auroraDatabaseCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of("auroraReplicaPerHour", 0.26));
        when(awsDynamoDBCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of("dynamoGlobalTablePerWruUsd", 0.000975));


        final Map<String, Object> result = calculator.calculateDatabaseBackupPricing();


        assertThat(result).isNotNull();
        assertThat((double) result.get("s3StandardStoragePerGbUsd")).isEqualTo(0.3);
        assertThat((double) result.get("rdsSnapshotStoragePerGbUsd")).isEqualTo(0.095);
        assertThat((double) result.get("auroraReplicaPerHour")).isEqualTo(0.26);
        assertThat((double) result.get("dynamoGlobalTablePerWruUsd")).isEqualTo(0.000975);
    }

    @Test
    void calculateDatabaseBackupPricing_apiFailures_usesFallbacks() throws Exception {

        when(pricingMock.getProducts(any(GetProductsRequest.class)))
                .thenThrow(new RuntimeException("AWS API Unavailable"));

        when(auroraDatabaseCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of("auroraReplicaPerHour", 0.26));
        when(awsDynamoDBCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of("dynamoGlobalTablePerWruUsd", 0.000975));


        final Map<String, Object> result = calculator.calculateDatabaseBackupPricing();


        assertThat(result).isNotNull();
        assertThat((double) result.get("s3StandardStoragePerGbUsd")).isGreaterThan(0);
        assertThat((double) result.get("rdsSnapshotStoragePerGbUsd")).isGreaterThan(0);
        assertThat((double) result.get("auroraReplicaPerHour")).isEqualTo(0.26);
        assertThat((double) result.get("dynamoGlobalTablePerWruUsd")).isEqualTo(0.000975);
    }

    private void setupElementsStubbing(JsonNode root, double value) {
        final JsonNode terms = mock(JsonNode.class);
        final JsonNode onDemand = mock(JsonNode.class);
        final JsonNode termValue = mock(JsonNode.class);
        final JsonNode priceDimensions = mock(JsonNode.class);
        final JsonNode dimension = mock(JsonNode.class);
        final JsonNode pricePerUnit = mock(JsonNode.class);
        final JsonNode usd = mock(JsonNode.class);

        lenient().when(root.path("terms")).thenReturn(terms);
        lenient().when(terms.path("OnDemand")).thenReturn(onDemand);
        lenient().when(onDemand.isMissingNode()).thenReturn(false);
        lenient().when(onDemand.isEmpty()).thenReturn(false);

        final Iterator<JsonNode> onDemandIterator = mock(Iterator.class);
        lenient().when(onDemand.elements()).thenReturn(onDemandIterator);
        lenient().when(onDemandIterator.next()).thenReturn(termValue);

        lenient().when(termValue.path("priceDimensions")).thenReturn(priceDimensions);
        lenient().when(priceDimensions.isMissingNode()).thenReturn(false);
        lenient().when(priceDimensions.isEmpty()).thenReturn(false);

        final Iterator<JsonNode> dimensionsIterator = mock(Iterator.class);
        lenient().when(priceDimensions.elements()).thenReturn(dimensionsIterator);
        lenient().when(dimensionsIterator.next()).thenReturn(dimension);

        lenient().when(dimension.path("pricePerUnit")).thenReturn(pricePerUnit);
        lenient().when(pricePerUnit.path("USD")).thenReturn(usd);
        lenient().when(usd.asDouble(anyDouble())).thenReturn(value);
    }

    private void injectMocks(AWSDatabaseCostCalculatorAdapter calc) throws Exception {

        final Field pricingField = AWSCloudCalculatorAdapter.class.getDeclaredField("pricingClient");
        pricingField.setAccessible(true);
        pricingField.set(calc, pricingMock);

        final Field mapperField = AWSCloudCalculatorAdapter.class.getSuperclass().getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(calc, mapperMock);

        final Field auroraField = AWSDatabaseCostCalculatorAdapter.class.getDeclaredField("auroraDatabaseCostCalculator");
        auroraField.setAccessible(true);
        auroraField.set(calc, auroraDatabaseCostCalculatorMock);

        final Field dynamoField = AWSDatabaseCostCalculatorAdapter.class.getDeclaredField("awsDynamoDBCostCalculator");
        dynamoField.setAccessible(true);
        dynamoField.set(calc, awsDynamoDBCostCalculatorMock);
    }
}
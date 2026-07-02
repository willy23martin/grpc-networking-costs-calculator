package com.calculator.cost.cloud.database.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.aws.AWSAuroraDatabaseCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.aws.AWSDatabaseCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.aws.AWSDynamoDBCostCalculator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AWSDatabaseCostCalculatorTest {

    @Mock
    private PricingClient pricingMock;
    @Mock
    private ObjectMapper mapperMock;

    @Mock
    private AWSAuroraDatabaseCostCalculator auroraDatabaseCostCalculatorMock;

    @Mock
    private AWSDynamoDBCostCalculator awsDynamoDBCostCalculatorMock;

    private AWSDatabaseCostCalculator calculator;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new AWSDatabaseCostCalculator();
        injectMocks(calculator);
    }

    @Test
    void calculateDatabaseBackupPricing_successfulApiResponse_returnsDynamicPrices() throws Exception {
       GetProductsResponse mockResponse = GetProductsResponse.builder()
                .priceList(List.of("{\"mock\":\"json\"}"))
                .build();

       when(pricingMock.getProducts(any(GetProductsRequest.class))).thenReturn(mockResponse);

        JsonNode s3Root = mock(JsonNode.class);
        JsonNode rdsRoot = mock(JsonNode.class);
        when(mapperMock.readTree(any(String.class))).thenReturn(s3Root, rdsRoot);

        setupElementsStubbing(s3Root, 0.023);
        setupElementsStubbing(rdsRoot, 0.095);

        when(auroraDatabaseCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of("auroraReplicaPerHour", 0.26));

        String expectedNote = String.format(java.util.Locale.US, "Add ~$%.6f/WRU per extra replication region beyond the primary.", 0.000975);
        when(awsDynamoDBCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of(
                        "dynamoGlobalTablePerWruUsd", 0.000975,
                        "dynamoGlobalTableNote", expectedNote
                ));

        Map<String, Object> result = calculator.calculateDatabaseBackupPricing();

        assertThat(result).isNotNull();
        assertThat(result.get("s3StandardStoragePerGbUsd")).isEqualTo(0.023);
        assertThat(result.get("rdsSnapshotStoragePerGbUsd")).isEqualTo(0.095);
        assertThat(result.get("auroraReplicaPerHour")).isEqualTo(0.26);
        assertThat(result.get("dynamoGlobalTablePerWruUsd")).isEqualTo(0.000975);
        assertThat((String) result.get("dynamoGlobalTableNote")).contains("0.000975");
    }

    @Test
    void calculateDatabaseBackupPricing_emptyPriceList_usesFallbacks() throws Exception {

        GetProductsResponse emptyResponse = GetProductsResponse.builder()
                .priceList(Collections.emptyList())
                .build();

        when(pricingMock.getProducts(any(GetProductsRequest.class))).thenReturn(emptyResponse);

        when(auroraDatabaseCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of("auroraReplicaPerHour", 0.26));
        when(awsDynamoDBCostCalculatorMock.calculateDatabaseBackupPricing())
                .thenReturn(Map.of("dynamoGlobalTablePerWruUsd", 0.000975));


        Map<String, Object> result = calculator.calculateDatabaseBackupPricing();


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


        Map<String, Object> result = calculator.calculateDatabaseBackupPricing();


        assertThat(result).isNotNull();
        assertThat((double) result.get("s3StandardStoragePerGbUsd")).isGreaterThan(0);
        assertThat((double) result.get("rdsSnapshotStoragePerGbUsd")).isGreaterThan(0);
        assertThat((double) result.get("auroraReplicaPerHour")).isEqualTo(0.26);
        assertThat((double) result.get("dynamoGlobalTablePerWruUsd")).isEqualTo(0.000975);
    }

    private void setupElementsStubbing(JsonNode root, double value) {
        JsonNode terms = mock(JsonNode.class);
        JsonNode onDemand = mock(JsonNode.class);
        JsonNode termValue = mock(JsonNode.class);
        JsonNode priceDimensions = mock(JsonNode.class);
        JsonNode dimension = mock(JsonNode.class);
        JsonNode pricePerUnit = mock(JsonNode.class);
        JsonNode usd = mock(JsonNode.class);

        lenient().when(root.path("terms")).thenReturn(terms);
        lenient().when(terms.path("OnDemand")).thenReturn(onDemand);
        lenient().when(onDemand.isMissingNode()).thenReturn(false);
        lenient().when(onDemand.isEmpty()).thenReturn(false);

        Iterator<JsonNode> onDemandIterator = mock(Iterator.class);
        lenient().when(onDemand.elements()).thenReturn(onDemandIterator);
        lenient().when(onDemandIterator.next()).thenReturn(termValue);

        lenient().when(termValue.path("priceDimensions")).thenReturn(priceDimensions);
        lenient().when(priceDimensions.isMissingNode()).thenReturn(false);
        lenient().when(priceDimensions.isEmpty()).thenReturn(false);

        Iterator<JsonNode> dimensionsIterator = mock(Iterator.class);
        lenient().when(priceDimensions.elements()).thenReturn(dimensionsIterator);
        lenient().when(dimensionsIterator.next()).thenReturn(dimension);

        lenient().when(dimension.path("pricePerUnit")).thenReturn(pricePerUnit);
        lenient().when(pricePerUnit.path("USD")).thenReturn(usd);
        lenient().when(usd.asDouble(anyDouble())).thenReturn(value);
    }

    private void injectMocks(AWSDatabaseCostCalculator calc) throws Exception {

        Field pricingField = AWSCloudCalculator.class.getDeclaredField("pricingClient");
        pricingField.setAccessible(true);
        pricingField.set(calc, pricingMock);

        Field mapperField = AWSCloudCalculator.class.getSuperclass().getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(calc, mapperMock);

        Field auroraField = AWSDatabaseCostCalculator.class.getDeclaredField("auroraDatabaseCostCalculator");
        auroraField.setAccessible(true);
        auroraField.set(calc, auroraDatabaseCostCalculatorMock);

        Field dynamoField = AWSDatabaseCostCalculator.class.getDeclaredField("awsDynamoDBCostCalculator");
        dynamoField.setAccessible(true);
        dynamoField.set(calc, awsDynamoDBCostCalculatorMock);
    }
}
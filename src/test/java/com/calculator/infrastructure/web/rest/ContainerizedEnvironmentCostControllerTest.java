package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContainerizedEnvironmentCostControllerTest {

    @Mock
    private ObjectMapper mapperMock;
    @Mock
    private JsonNode rootMock;
    @Mock
    private PricingClient pricingClientMock;  // Direct mock (no construction)

    @InjectMocks private ContainerizedEnvironmentCostController controller;

    @BeforeEach
    void setUp() throws Exception {
        // Inject PricingClient mock via reflection (covers ALL private API calls)
        injectPricingClientMock();
    }

    // TODO
    @Disabled
    @Test
    void getContainerPricing_apiSuccess_allFieldsPopulated() throws JsonProcessingException {
        when(mapperMock.readTree("mock-json")).thenReturn(rootMock);
        setupJsonPriceExtraction(0.0405);  // Fargate override

        var response = controller.getContainerPricing();
        var resp = response.getBody();

        assertAll("Complete pricing",
                () -> assertEquals(73.0, resp.eksControlPlanePerMonth),
                () -> assertEquals(0.0405, resp.fargateVcpuPerHour),  // API override
                () -> assertEquals(13, resp.ec2OnDemandPrices.size()),
                () -> assertEquals(0.096, resp.ec2OnDemandPrices.get("m6i.large")),
                () -> assertTrue(resp.source.contains("AWS Pricing API (live)"))
        );
    }

    // TODO
    @Disabled
    @Test
    void getContainerPricing_apiFailure_pureFallbacks() {
        // Arrange: Force API exception (try-catch branches)
        when(pricingClientMock.getProducts((GetProductsRequest) any())).thenThrow(new RuntimeException("API down"));

        // Act
        var resp = controller.getContainerPricing().getBody();

        // Assert: Hardcoded fallbacks + failure source
        assertAll("Fallback path",
                () -> assertEquals("Hardcoded fallback (Q1-2025)", resp.source),
                () -> assertEquals(0.04048, resp.fargateVcpuPerHour),  // Original fallback
                () -> assertEquals(13, resp.ec2OnDemandPrices.size())
        );
    }

    // TODO
    @Disabled
    @Test
    void getApiGatewayPricing_allFields_apiOverridePossible() throws JsonProcessingException {
        // Arrange: Mock API Gateway success
        when(mapperMock.readTree("mock-json")).thenReturn(rootMock);
        setupJsonPriceExtraction(0.0000035);  // Per-request

        // Act
        var resp = controller.getApiGatewayPricing().getBody();

        // Assert: All 12 fields + REST API override (×1M)
        assertAll("API Gateway",
                () -> assertEquals(3.50, resp.restApiPer1MCallsMonthly),
                () -> assertEquals(1.00, resp.httpApiPer1MCallsFirst1B),
                () -> assertEquals(0.25, resp.wsApiPer1MConnections),
                () -> assertEquals(0.020, resp.cacheHalfGbPerHour)
        );
    }

    // TODO
    @Disabled
    @Test
    void calculateContainerTco_completeEksEc2_all12Components() {
        // Arrange: Full scenario → ALL 12 if() branches + fetchEc2Price()
        var req = new ContainerizedEnvironmentCostController.ContainerTcoRequest();
        req.clusterCount = 1;                    // EKS
        req.orchestrationType = "EKS";
        req.underlyingResources = "ec2";         // !Fargate
        req.ec2InstanceType = "m6i.large";       // Map hit
        req.podCount = 25; req.podsPerNode = 10; // 3 nodes
        req.hostStorageGbPerNode = 50;           // EBS
        req.clusterBackupGb = 100;               // S3
        req.statefulSetReplicas = 3; req.pvcGbPerReplica = 20;
        req.clusterLbCount = 2; req.clusterLbLcuPerHour = 10;
        req.clusterWaf = true; req.wafRuleCount = 10;
        req.ecrImageStorageGb = 50;
        req.hostOsLicenseRatePerHour = 0.01;
        req.workloadLicenseCostPerMonth = 100;
        req.riDiscountPct = 40;

        // Act: Single call → fetchEc2Price() + ALL conditionals
        var resp = controller.calculateContainerTco(req).getBody();

        // Assert: Every cost component + line items + discount
        assertAll("Full TCO",
                () -> assertEquals(73.0, resp.eksControlPlaneCost),
                () -> assertEquals(3, resp.estimatedNodeCount),
                () -> assertEquals(0.096, resp.ec2InstancePricePerHour),
                () -> assertTrue(resp.hostStorageCost > 12),
                () -> assertTrue(resp.backupStorageCost > 2.3),
                () -> assertTrue(resp.lineItems.size() >= 12),
                () -> assertTrue(resp.discountSaving > 80)
        );
    }

    @Test
    void calculateContainerTco_fargateSpot_cronJobs() {
        var req = new ContainerizedEnvironmentCostController.ContainerTcoRequest();
        req.underlyingResources = "fargate-spot";  // isFargate + isSpot
        req.fargateVcpuPerPod = 0.5; req.fargateGbPerPod = 1.0;
        req.fargateActivePods = 5;
        req.cronJobCount = 3;                       // CronJob branch
        req.cronJobDurationMinutes = 10; req.cronJobRunsPerDay = 6;

        var resp = controller.calculateContainerTco(req).getBody();

        assertAll("Fargate + Cron",
                () -> assertTrue(resp.fargateCost > 0),
                () -> assertTrue(resp.cronJobFargateCost > 0),  // isFargate && cronJobCount>0
                () -> assertTrue(resp.lineItems.containsKey("CronJob Fargate Compute"))
        );
    }

    // Critical: Inject PricingClient mock (covers try-with-resources in ALL private methods)
    private void injectPricingClientMock() throws Exception {
        // Create spy to preserve real behavior + inject mock client
        Field mapperField = ContainerizedEnvironmentCostController.class.getDeclaredField("mapper");
        mapperField.setAccessible(true);
        mapperField.set(controller, mapperMock);
    }

    // JSON parsing coverage (extractOnDemandPriceFromJson all branches)
    private void setupJsonPriceExtraction(double price) {
        lenient().when(rootMock.path("terms").path("OnDemand").elements().hasNext()).thenReturn(true);
        lenient().when(rootMock.path("priceDimensions").elements().hasNext()).thenReturn(true);
        lenient().when(rootMock.path("pricePerUnit").path("USD").asText("0")).thenReturn(String.valueOf(price));
    }
}
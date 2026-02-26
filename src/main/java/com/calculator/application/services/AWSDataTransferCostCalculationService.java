package com.calculator.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Service
public class AWSDataTransferCostCalculationService {


    @Value("${aws.pricing.ec2.rates}")
    List<Double> fallbackRates;

    private final PricingClient pricingClient;

    private Instant cacheExpiry = Instant.MIN;

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final Logger log = Logger.getLogger(AWSDataTransferCostCalculationService.class.getName());

    // Based on the ones defined https://aws.amazon.com/ec2/pricing/on-demand/ up to date:
    private final static double[] AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB = {10_240.0, 40_960.0, 102_400.0, Double.MAX_VALUE };
    
    public AWSDataTransferCostCalculationService() {
        this.pricingClient = PricingClient.builder()
                .region(Region.US_EAST_1) // Because AWS Pricing API is only available in us-east-1
                .build();
    }

    public double calculateDataTransferCost(double responseGbPerMonth) {
        List<Double> rates = getDataTransferRates();

        double cost = 0.0;
        double remainingResponseGbPerMonth = responseGbPerMonth;

        for (int i = 0; i < AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB.length && remainingResponseGbPerMonth > 0; i++) {
            double tierSize = Math.min(remainingResponseGbPerMonth, AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[i]);
            double rate = (i < rates.size()) ? rates.get(i) : rates.get(rates.size() - 1);
            cost += tierSize * rate;
            remainingResponseGbPerMonth -= tierSize;
        }

        return cost;
    }

    private List<Double> getDataTransferRates() {
        List<Double> cachedRates = null;
        if (cachedRates == null || Instant.now().isAfter(cacheExpiry)) {
            cachedRates = fetchDataTransferOutTiers();
            cacheExpiry = Instant.now().plus(CACHE_TTL);
        }
        return cachedRates;
    }

    private List<Double> fetchDataTransferOutTiers() {
        try {
        List<Filter> filters = Arrays.asList(
                Filter.builder()
                        .type(FilterType.TERM_MATCH)
                        .field("serviceCode")
                        .value("AWSDataTransfer")
                        .build(),
                Filter.builder()
                        .type(FilterType.TERM_MATCH)
                        .field("productFamily")
                        .value("Data Transfer")
                        .build(),
                Filter.builder()
                        .type(FilterType.TERM_MATCH)
                        .field("transferType")
                        .value("AWS Outbound")
                        .build(),
                Filter.builder()
                        .type(FilterType.TERM_MATCH)
                        .field("fromLocation")
                        .value("US East (N. Virginia)")
                        .build(),
                Filter.builder()
                        .type(FilterType.TERM_MATCH)
                        .field("toLocation")
                        .value("External")
                        .build()
        );

        GetProductsRequest request = GetProductsRequest.builder()
                .serviceCode("AWSDataTransfer")
                .filters(filters)
                .formatVersion("aws_v1")
                .maxResults(10)
                .build();

        GetProductsResponse response = pricingClient.getProducts(request);
        log.info("AWS Pricing API has been used for rates");
        return parseTieredRates(response.priceList());
    } catch (Exception e) {
            log.info("AWS Pricing API unavailable, using fallback rates: " + e.getMessage());
            return fallbackRates;
        }
    }

    private List<Double> parseTieredRates(List<String> priceList) {
        List<Double> rates = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        for (String priceJson : priceList) {
            try {
                JsonNode root = mapper.readTree(priceJson);

                root.path("terms")
                        .path("OnDemand")
                        .findValues("pricePerUnit")
                        .forEach(priceNode -> {
                            double usdRate = priceNode.path("USD").asDouble();
                            if (usdRate > 0) {
                                rates.add(usdRate);
                            }
                        });

            } catch (Exception e) {
                log.info("Failed to parse price Tiered Rates JSON: " + e.getMessage());
            }
        }

        Collections.sort(rates, Collections.reverseOrder());
        return rates;
    }
}

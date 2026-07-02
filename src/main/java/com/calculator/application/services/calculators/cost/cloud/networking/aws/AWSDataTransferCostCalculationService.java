package com.calculator.application.services.calculators.cost.cloud.networking.aws;

import com.calculator.application.services.calculators.cost.cloud.networking.NetworkingCostCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static com.calculator.shared.JSONLogger.logAsJSON;

public class AWSDataTransferCostCalculationService implements NetworkingCostCalculator {

    @Value("${aws.pricing.ec2.rates}")
    private List<Double> fallbackRates;

    @Autowired
    private PricingClient pricingClient;

    private Instant cacheExpiry = Instant.MIN;

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final Logger log = Logger.getLogger(AWSDataTransferCostCalculationService.class.getName());

    // Based on the ones defined https://aws.amazon.com/ec2/pricing/on-demand/ up to date:
    public final static double[] AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB = {10_240.0, 40_960.0, 102_400.0, Double.MAX_VALUE };

    @Getter
    private List<Double> dataTransferRates;

    @Override
    public double calculateDataTransferCost(double responseGbPerMonth) {

        double cost = 0.0;
        double remainingResponseGbPerMonth = responseGbPerMonth;

        for (int i = 0; i < AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB.length && remainingResponseGbPerMonth > 0; i++) {
            double tierSize = Math.min(remainingResponseGbPerMonth, AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[i]);
            double rate = (i < dataTransferRates.size()) ? dataTransferRates.get(i) : dataTransferRates.getLast();
            cost += tierSize * rate;
            remainingResponseGbPerMonth -= tierSize;
        }

        return cost;
    }

    private List<Double> fetchDataTransferRates() {
        List<Double> cachedRates;
        cachedRates = fetchDataTransferOutTiers();
        cacheExpiry = Instant.now().plus(CACHE_TTL);
        dataTransferRates = cachedRates;
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
            logAsJSON(log, response);
            return parseTieredRates(response.priceList());
    } catch (Exception e) {
            log.severe("AWS Pricing API unavailable, using fallback rates: " + e.getMessage());
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
                log.severe("Failed to parse price Tiered Rates JSON: " + e.getMessage());
            }
        }

        rates.sort(Collections.reverseOrder());
        log.info(rates.toString()); // [0.09, 0.085, 0.07, 0.05]
        return rates;
    }

    @PostConstruct
    private void init() {
        fetchDataTransferRates();
    }

}

package com.calculator.infrastructure.cloud.adapters.aws.caching;

import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.calculator.application.services.calculators.cost.cloud.ports.CachingCostCalculatorPort;
import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public class AWSCachingCostCalculatorAdapter extends AWSCloudCalculatorAdapter implements CachingCostCalculatorPort {

    public AWSCachingCostCalculatorAdapter(PricingClient pricingClient) {
        super(pricingClient);
    }

    @Override
    public Map<String, Object> calculateCachingCosts() {
        Map<String, Object> cachingCosts = new LinkedHashMap<>();
        mapCacheRG6Large(cachingCosts);
        mapMemcached(cachingCosts, "memcachedR6gLargePerHour", "memcached", "memcachedR6gXlargePerHour");
        mapBackupStorageForRedisSnapshots(cachingCosts);
        return cachingCosts;
    }

    private static void mapBackupStorageForRedisSnapshots(Map<String, Object> cachingCosts) {
        cachingCosts.put("snapshotStoragePerGbMonth", 0.085);
        cachingCosts.put("note","ElastiCache on-demand prices shown. Reserved Nodes offer up to 55% savings (1yr) or 70% (3yr) for committed workloads.");
    }

    private void mapMemcached(Map<String, Object> cachingCosts, String memcachedR6gLargePerHour, String memcached, String memcachedR6gXlargePerHour) {
        cachingCosts.put(memcachedR6gLargePerHour, fetchElastiCachePrice("cache.r6g.large", memcached));
        cachingCosts.put(memcachedR6gXlargePerHour, fetchElastiCachePrice("cache.r6g.xlarge", memcached));
    }

    private void mapCacheRG6Large(Map<String, Object> cachingCosts) {
        mapMemcached(cachingCosts, "redisR6gLargePerHour", "redis", "redisR6gXlargePerHour");
        cachingCosts.put("redisR6g2xlargePerHour",     fetchElastiCachePrice("cache.r6g.2xlarge", "redis"));
    }

    private double fetchElastiCachePrice(String nodeType, String engine) {
        try {
            GetProductsRequest productsRequest = GetProductsRequest.builder()
                    .serviceCode("AmazonElastiCache")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("cacheEngine").value(engine).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value(nodeType).build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();
            GetProductsResponse resp = pricingClient.getProducts(productsRequest);
            if (resp.priceList().isEmpty()) return 0.0;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            return root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(0.0);
        } catch (Exception e) {
            Map<String,Double> fallBackCosts = Map.of("cache.r6g.large",0.166,"cache.r6g.xlarge",0.332,"cache.r6g.2xlarge",0.665);
            return fallBackCosts.getOrDefault(nodeType, 0.20);
        }
    }
}

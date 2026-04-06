package com.calculator.application.services.calculators.cost.cloud.caching.aws.elasticache;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.caching.CachingCostCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public class ElastiCacheCostCalculator extends AWSCloudCalculator implements CachingCostCalculator {

    @Override
    public Map<String, Object> calculateCachingCosts() {
        Map<String, Object> result = new LinkedHashMap<>();
        // ElastiCache Redis — cache.r6g.large, us-east-1 on-demand
        result.put("redisR6gLargePerHour",       fetchElastiCachePrice("cache.r6g.large",   "redis"));
        result.put("redisR6gXlargePerHour",      fetchElastiCachePrice("cache.r6g.xlarge",  "redis"));
        result.put("redisR6g2xlargePerHour",     fetchElastiCachePrice("cache.r6g.2xlarge", "redis"));
        // ElastiCache Memcached
        result.put("memcachedR6gLargePerHour",   fetchElastiCachePrice("cache.r6g.large",   "memcached"));
        result.put("memcachedR6gXlargePerHour",  fetchElastiCachePrice("cache.r6g.xlarge",  "memcached"));
        // Backup storage for Redis snapshots — same as S3 standard
        result.put("snapshotStoragePerGbMonth", 0.085);
        result.put("note","ElastiCache on-demand prices shown. Reserved Nodes offer up to 55% savings (1yr) or 70% (3yr) for committed workloads.");
        return result;
    }

    private double fetchElastiCachePrice(String nodeType, String engine) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonElastiCache")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("cacheEngine").value(engine).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value(nodeType).build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();
            GetProductsResponse resp = pricing.getProducts(req);
            if (resp.priceList().isEmpty()) return 0.0;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            return root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(0.0);
        } catch (Exception e) {
            Map<String,Double> fb = Map.of("cache.r6g.large",0.166,"cache.r6g.xlarge",0.332,"cache.r6g.2xlarge",0.665);
            return fb.getOrDefault(nodeType, 0.20);
        }
    }
}

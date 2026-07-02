package com.calculator.application.configuration;

import com.calculator.application.services.calculators.cost.cloud.alb.ALBCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.alb.aws.alb.AWSALBCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.caching.CachingCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.caching.aws.elasticache.ElastiCacheCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.compute.CloudComputeCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.compute.aws.ec2.EC2ComputeCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.DatabaseCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.aws.AWSDatabaseCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.finops.FinOpsStrategyCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.finops.aws.AWSFinOpsStrategyCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.networking.aws.AWSDataTransferCostCalculationService;
import com.calculator.application.services.calculators.cost.cloud.networking.NetworkingCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.SecurityCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.aws.AWSSecurityCostCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;

import java.time.Duration;

@Configuration
public class CostCalculatorsConfiguration {

    @Bean
    @Profile(CloudProvider.AWS)
    public PricingClient pricingClient(){
        return PricingClient.builder()
                .region(Region.US_EAST_1) // Because AWS Pricing API is only available in us-east-1
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(builder -> builder
                        .apiCallAttemptTimeout(Duration.ofSeconds(4))
                        .apiCallTimeout(Duration.ofSeconds(10))
                )
                .build();
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public NetworkingCostCalculator networkingCostCalculator(){
        NetworkingCostCalculator networkingCostCalculator = new AWSDataTransferCostCalculationService();
        return networkingCostCalculator;
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public CloudComputeCostCalculator cloudComputeCostCalculator() {
        CloudComputeCostCalculator cloudComputeCostCalculator = new EC2ComputeCostCalculator();
        return cloudComputeCostCalculator;
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public ALBCostCalculator albCostCalculator(){
        ALBCostCalculator albCostCalculator = new AWSALBCostCalculator();
        return albCostCalculator;
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public DatabaseCostCalculator databaseCostCalculator() {
        DatabaseCostCalculator databaseCostCalculator = new AWSDatabaseCostCalculator();
        return databaseCostCalculator;
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public SecurityCostCalculator securityCostCalculator() {
        SecurityCostCalculator securityCostCalculator = new AWSSecurityCostCalculator();
        return securityCostCalculator;
    }

    @Bean
    @Profile(CloudProvider.AWS)
    FinOpsStrategyCostCalculator finOpsStrategyCostCalculator(){
        FinOpsStrategyCostCalculator finOpsStrategyCostCalculator = new AWSFinOpsStrategyCostCalculator();
        return finOpsStrategyCostCalculator;
    }

    @Bean
    @Profile(CloudProvider.AWS)
    CachingCostCalculator cachingCostCalculator() {
        CachingCostCalculator cachingCostCalculator = new ElastiCacheCostCalculator();
        return cachingCostCalculator;
    }

}

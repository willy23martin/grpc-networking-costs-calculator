package com.calculator.infrastructure.cloud.adapters.aws.configuration;

import com.calculator.application.services.calculators.cost.cloud.ports.*;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.finops.reliability.FinOpsStrategyReliabilityArchitecturalDecisionRepository;
import com.calculator.infrastructure.cloud.adapters.CloudProvider;
import com.calculator.infrastructure.cloud.adapters.aws.alb.AWSALBCostCalculatorAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.caching.AWSCachingCostCalculatorAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.compute.AWSComputeCostCalculatorAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.containers.AWSContainersCostCalculatorAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.database.AWSDatabaseCostCalculatorAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.finops.AWSFinOpsStrategyCostCalculatorAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.networking.AWSDataTransferCostCalculationServiceAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.security.AWSSecurityCostCalculatorAdapter;
import com.calculator.infrastructure.cloud.adapters.aws.security.AWSSecurityWAFCostCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;

import java.time.Duration;

@Configuration
public class AWSCostCalculatorAdaptersConfiguration {

    // Because AWS Pricing API is only available in us-east-1
    @Bean
    @Profile(CloudProvider.AWS)
    public PricingClient pricingClient(){
        return PricingClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(builder -> builder
                        .apiCallAttemptTimeout(Duration.ofSeconds(4))
                        .apiCallTimeout(Duration.ofSeconds(10))
                )
                .build();
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public NetworkingCostCalculatorPort networkingCostCalculator(PricingClient pricingClient){
        return new AWSDataTransferCostCalculationServiceAdapter(pricingClient);
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public CloudComputeCostCalculatorPort cloudComputeCostCalculator(PricingClient pricingClient) {
        return new AWSComputeCostCalculatorAdapter(pricingClient);
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public ALBCostCalculatorPort albCostCalculator(
            PricingClient pricingClient,
            FinOpsStrategyReliabilityArchitecturalDecisionRepository finOpsStrategyReliabilityArchitecturalDecisionRepository
    ){
        return new AWSALBCostCalculatorAdapter(pricingClient, finOpsStrategyReliabilityArchitecturalDecisionRepository);
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public DatabaseCostCalculatorPort databaseCostCalculator(PricingClient pricingClient) {
        return new AWSDatabaseCostCalculatorAdapter(pricingClient);
    }

    @Bean
    @Profile(CloudProvider.AWS)
    public SecurityCostCalculatorPort securityCostCalculator(
            PricingClient pricingClient,
            AWSSecurityWAFCostCalculator awsSecurityWAFCostCalculator
    ) {
        return  new AWSSecurityCostCalculatorAdapter(pricingClient, awsSecurityWAFCostCalculator);
    }

    @Bean
    @Profile(CloudProvider.AWS)
    FinOpsStrategyCostCalculatorPort finOpsStrategyCostCalculator(PricingClient pricingClient){
        return new AWSFinOpsStrategyCostCalculatorAdapter(pricingClient);
    }

    @Bean
    @Profile(CloudProvider.AWS)
    CachingCostCalculatorPort cachingCostCalculator(PricingClient pricingClient) {
        return new AWSCachingCostCalculatorAdapter(pricingClient);
    }

    @Bean
    @Profile(CloudProvider.AWS)
    ContainerizedCostCalculatorPort containerizedCostCalculatorPort(PricingClient pricingClient) {
        return new AWSContainersCostCalculatorAdapter(pricingClient);
    }


}

package com.calculator.domain.model.architecture;

import com.calculator.domain.model.architecture.cloud.CloudProvider;
import com.calculator.domain.model.cost.CostFactor;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class CloudService extends ArchitecturalDecision {

    private CloudProvider cloudProvider;
    private String name;
    private ArchitecturalCharacteristic architecturalCharacteristic;
    private ArchitecturalTactic supportedArchitecturalTactic;
    private InfrastructureCost infrastructureCost;
    private List<ArchitecturalDecision> supportedArchitecturalDecisions;

    @Builder
    public CloudService(CloudProvider cloudProvider,
                        String id,
                        String name,
                        ArchitecturalCharacteristic architecturalCharacteristic,
                        ArchitecturalTactic supportedArchitecturalTactic,
                        InfrastructureCost infrastructureCost,
                        List<ArchitecturalDecision> supportedArchitecturalDecisions,
                        CostFactor costFactor
    ) {

        super(id, architecturalCharacteristic, costFactor);

        this.cloudProvider = cloudProvider;
        this.name = name;
        this.architecturalCharacteristic = architecturalCharacteristic;
        this.supportedArchitecturalTactic = supportedArchitecturalTactic;
        this.infrastructureCost = infrastructureCost;

        this.supportedArchitecturalDecisions = supportedArchitecturalDecisions != null
                ? supportedArchitecturalDecisions
                : new ArrayList<>(1);
    }

    public static class CloudServiceBuilder {
        private CloudProvider cloudProvider;
        private String id;
        private String name;
        private ArchitecturalCharacteristic architecturalCharacteristic;
        private ArchitecturalTactic supportedArchitecturalTactic;
        private InfrastructureCost infrastructureCost;
        private List<ArchitecturalDecision> supportedArchitecturalDecisions;
        private CostFactor costFactor;

        public CloudService build() {
            List<ArchitecturalDecision> aggregatedDecisions;

            if (this.supportedArchitecturalDecisions == null) {
                aggregatedDecisions = new ArrayList<>(1);
            } else {
                aggregatedDecisions = new ArrayList<>(this.supportedArchitecturalDecisions);
            }

            if (this.supportedArchitecturalTactic != null && !aggregatedDecisions.contains(this.supportedArchitecturalTactic)) {
                aggregatedDecisions.add(this.supportedArchitecturalTactic);
            }

            return new CloudService(
                    this.cloudProvider,
                    this.id,
                    this.name,
                    this.architecturalCharacteristic,
                    this.supportedArchitecturalTactic,
                    this.infrastructureCost,
                    aggregatedDecisions,
                    this.costFactor
            );
        }
    }
}
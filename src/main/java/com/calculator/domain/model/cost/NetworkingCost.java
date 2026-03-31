package com.calculator.domain.model.cost;

import lombok.*;

import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@Getter
@Builder
public final class NetworkingCost extends CostFactor {

    private String criteria;

    @Builder.Default
    private long value = 0L;

    private String operation;

    private String units;

    public long operate(long baseValue) {
        Optional<String> optionalOperation = Optional.ofNullable(this.operation);
        if(optionalOperation.isPresent()) {
            if (operation.equals(CostOperations.MULTIPLIER.name())) {
                return baseValue * this.value;
            } else {
                return baseValue;
            }
        } else {
            return baseValue;
        }
    }

}

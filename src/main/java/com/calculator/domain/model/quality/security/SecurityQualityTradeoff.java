package com.calculator.domain.model.quality.security;

import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import lombok.Getter;

@Getter
public class SecurityQualityTradeoff extends QualityTradeoff {

    private String[] linkedOwaspTop10Vulnerabilities;
    private String[] linkedOwaspLabels;
    private String[] cwes;
    private String vulnerabilityPrevented;

    public SecurityQualityTradeoff(
            ArchitecturalCharacteristic architecturalCharacteristic,
            TradeoffType tradeoffType,
            String[] linkedOwaspTop10Vulnerabilities,
            String[] linkedOwaspLabels,
            String[] cwes,
            String vulnerabilityPrevented
    ) {
        super(architecturalCharacteristic, tradeoffType);
        this.linkedOwaspTop10Vulnerabilities = linkedOwaspTop10Vulnerabilities;
        this.linkedOwaspLabels = linkedOwaspLabels;
        this.cwes = cwes;
        this.vulnerabilityPrevented = vulnerabilityPrevented;
    }
}

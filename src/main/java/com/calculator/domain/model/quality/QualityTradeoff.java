package com.calculator.domain.model.quality;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QualityTradeoff {
    protected ArchitecturalCharacteristic architecturalCharacteristic;
    protected TradeoffType tradeoffType;
}

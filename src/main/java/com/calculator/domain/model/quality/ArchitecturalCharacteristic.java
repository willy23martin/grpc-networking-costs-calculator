package com.calculator.domain.model.quality;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@Builder
public class ArchitecturalCharacteristic {
    private String name;
    private List<QualityTradeoff> qualityTradeoffs;
}

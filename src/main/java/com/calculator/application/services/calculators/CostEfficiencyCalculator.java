package com.calculator.application.services.calculators;

import com.calculator.domain.dto.requests.TCOUnitEconomicsRequest;
import com.calculator.domain.dto.responses.TCOUnitEconomicsResponse;
import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.repository.cloud.reliability.CloudReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.resiliency.CloudResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.calculator.application.services.utils.MathUtils.*;

@Service
public class CostEfficiencyCalculator {

    public static final double SECONDS_PER_MONTH = 2_592_000.0;
    public static final int MONTHS_PER_YEAR = 12;
    public static final int DAYS_PER_MONTH = 30;
    public static final int HOURS_PER_MONTH = 730;

    @Autowired
    private SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;
    @Autowired
    private CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    @Autowired
    private ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;
    @Autowired
    private CloudReliabilityArchitecturalDecisionRepository cloudReliabilityArchitecturalDecisionRepository;

    @Autowired
    private ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository;
    @Autowired
    private CloudResiliencyArchitecturalDecisionRepository cloudResiliencyArchitecturalDecisionRepository;

    private List<ArchitecturalDecision> architecturalDecisions; // TODO - Receive them from the controller the selected ones to calculate the unit economics

    private double egressTransferCostUsd = 0.0;
    private double cloudInfraCostUsd = 0.0;
    private double finopsSavingUsd = 0.0;
    private int effectiveRps = 0;
    private int consumerCount = 1;
    private double revenuePerUserPerMonth = 0.0;

    public TCOUnitEconomicsResponse calculateUnitEconomics(
            TCOUnitEconomicsRequest unitEconomicsRequest
    ) {

        setCostEfficiencyCalculationParameters(unitEconomicsRequest);

        TCOUnitEconomicsResponse unitEconomicsResponse = new TCOUnitEconomicsResponse();

        int serviceConsumers = Math.max(1, consumerCount);
        double totalCostOfOwnership = Math.max(0, egressTransferCostUsd + cloudInfraCostUsd);
        long monthlyRequests = effectiveRps * (long) SECONDS_PER_MONTH;

        calculateTCO(unitEconomicsResponse, totalCostOfOwnership, monthlyRequests);
        calculateUnitCosts(unitEconomicsResponse, monthlyRequests, totalCostOfOwnership, serviceConsumers);
        calculateROI(unitEconomicsResponse, serviceConsumers, totalCostOfOwnership);

        return unitEconomicsResponse;
    }

    private void setCostEfficiencyCalculationParameters(TCOUnitEconomicsRequest unitEconomicsRequest) {
        this.architecturalDecisions = getArchitecturalDecisions();
        this.egressTransferCostUsd = unitEconomicsRequest.egressTransferCostUsd;
        this.cloudInfraCostUsd = unitEconomicsRequest.cloudInfraCostUsd;
        this.finopsSavingUsd = unitEconomicsRequest.finopsSavingUsd;
        this.effectiveRps = unitEconomicsRequest.effectiveRps;
        this.consumerCount = unitEconomicsRequest.consumerCount;
        this.revenuePerUserPerMonth = unitEconomicsRequest.revenuePerUserPerMonth;
    }

    private void calculateROI(TCOUnitEconomicsResponse unitEconomicsResponse, int endUsers, double totalCostOfOwnership) {
        double revenue = revenuePerUserPerMonth;
        unitEconomicsResponse.hasRevenueData = revenue > 0;

        if (unitEconomicsResponse.hasRevenueData) {
            double totalMonthlyRevenue = revenue * endUsers;
            double netProfit = totalMonthlyRevenue - totalCostOfOwnership;

            unitEconomicsResponse.totalMonthlyRevenueUsd = round2(totalMonthlyRevenue);
            unitEconomicsResponse.totalAnnualRevenueUsd = round2(totalMonthlyRevenue * MONTHS_PER_YEAR);
            unitEconomicsResponse.arpuMonthly = round2(revenue);

            unitEconomicsResponse.monthlyRoiPct = totalCostOfOwnership > 0
                    ? round2(netProfit / totalCostOfOwnership * 100) : 0;
            unitEconomicsResponse.annualRoiPct = (totalCostOfOwnership > 0)
                    ? round2(((totalMonthlyRevenue * MONTHS_PER_YEAR)
                    - (totalCostOfOwnership * MONTHS_PER_YEAR))
                    / (totalCostOfOwnership * MONTHS_PER_YEAR) * 100)
                    : 0;
            unitEconomicsResponse.netMonthlyProfitUsd = round2(netProfit);
            unitEconomicsResponse.netAnnualProfitUsd = round2(netProfit * MONTHS_PER_YEAR);
            unitEconomicsResponse.breakEvenUsers = (revenue > 0 && totalCostOfOwnership > 0)
                    ? (int) Math.ceil(totalCostOfOwnership / revenue) : 0;
            unitEconomicsResponse.revenuePerDollarInfra = totalCostOfOwnership > 0
                    ? round2(totalMonthlyRevenue / totalCostOfOwnership) : 0;
            unitEconomicsResponse.netMarginPerUserMonthly = round4(revenue - unitEconomicsResponse.costPerUserPerMonthUsd);
        }
    }

    private void calculateUnitCosts(TCOUnitEconomicsResponse resp, long monthlyReqs, double totalTco, int endUsers) {
        resp.costPerRequestUsd = monthlyReqs > 0 ? round6(totalTco / monthlyReqs) : 0;
        resp.costPerUserPerMonthUsd = round4(totalTco / endUsers);
        resp.costPerUserPerDayUsd = round6(resp.costPerUserPerMonthUsd / DAYS_PER_MONTH);
    }

    private void calculateTCO(TCOUnitEconomicsResponse resp, double totalTco, long monthlyReqs) {
        resp.totalMonthlyTcoUsd = round2(totalTco);
        resp.totalAnnualTcoUsd = round2(totalTco * MONTHS_PER_YEAR);
        resp.egressCostUsd = round2(egressTransferCostUsd);
        resp.cloudInfraCostUsd = round2(cloudInfraCostUsd);
        resp.finopsSavingUsd = round2(finopsSavingUsd);
        resp.totalMonthlyRequests = monthlyReqs;
    }

    public TradeoffType summariseAffordabilityImpact() {
        boolean anyInhibits = false;
        boolean anyPromotes = false;

        for (ArchitecturalDecision decision : architecturalDecisions) {
            if (decision.getArchitecturalCharacteristic() == null) continue;
            List<?> tradeoffs = decision.getArchitecturalCharacteristic().getQualityTradeoffs();
            if (tradeoffs == null) continue;

            for (Object tradeoff : tradeoffs) {
                if (!(tradeoff instanceof com.calculator.domain.model.quality.QualityTradeoff qt)) continue;
                if (qt.getArchitecturalCharacteristic() == null) continue;

                String attrName = qt.getArchitecturalCharacteristic().getName();
                if (!ArchitecturalCharacteristics.AFFORDABILITY.name().equalsIgnoreCase(attrName)) continue;

                TradeoffType type = qt.getTradeoffType();
                if (type == TradeoffType.INHIBITS)  anyInhibits = true;
                if (type == TradeoffType.PROMOTES)  anyPromotes = true;
            }
        }

        if (anyInhibits) return TradeoffType.INHIBITS;
        if (anyPromotes) return TradeoffType.PROMOTES;
        return TradeoffType.ORTHOGONAL;
    }

    private List<ArchitecturalDecision> getArchitecturalDecisions() {
        List<ArchitecturalDecision> allDecisions = new ArrayList<>();

        allDecisions.addAll(reliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions());
        allDecisions.addAll(cloudReliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions());

        allDecisions.addAll(resiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions());
        allDecisions.addAll(cloudResiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions());

        allDecisions.addAll(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions());
        allDecisions.addAll(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions());
        return allDecisions;
    }
}
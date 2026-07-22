package com.calculator.application.services.mapper.tactics;

import com.calculator.application.services.calculators.cost.cloud.ports.NetworkingCostCalculatorPort;
import com.calculator.domain.dto.requests.TacticTCCContributionRequest;
import com.calculator.domain.dto.responses.TacticContributionItem;
import com.calculator.domain.model.architecture.tactics.security.TLSOverhead;
import com.calculator.domain.model.architecture.ArchitecturalPattern;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.architecture.tactics.security.JWTOverhead;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.security.SecurityQualityTradeoff;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.calculator.application.services.utils.MathUtils.round2;
import static com.calculator.application.services.calculators.CostEfficiencyCalculator.SECONDS_PER_MONTH;
import static com.calculator.infrastructure.web.rest.NetworkingCostCalculatorController.BYTES_PER_GB;

@Service
public class TacticsTCCContributionService {

    public static final String RPS_AND_BYTES = "both";
    public static final String RPS = "rps";
    public static final String BYTES = "bytes";
    @Autowired
    private ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;

    @Autowired
    private ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository;

    @Autowired
    private SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @Autowired
    NetworkingCostCalculatorPort networkingCostCalculator; // DESIGN PATTERNS: Port whose adapter is AWSDataTransferCostCalculationService class

    public List<TacticContributionItem> mapStructuralInformationalTacticsWithNoCostImpact(TacticTCCContributionRequest tacticContributionRequest) {
        List<TacticContributionItem> tacticContributionItems = new ArrayList<>();
        if(tacticContributionRequest.clientSideLoadBalancingEnabled) {
            tacticContributionItems.add(
                    TacticContributionItem.builder()
                            .label(((ArchitecturalTactic)reliabilityArchitecturalDecisionRepository.getClientSideLoadBalancing()).getName())
                            .build()
            );
        }

        if(tacticContributionRequest.serverSideLoadBalancingEnabled) {
            tacticContributionItems.add(
                    TacticContributionItem.builder()
                            .label(((ArchitecturalTactic)reliabilityArchitecturalDecisionRepository.getServerSideLoadBalancing()).getName())
                            .build()
            );
        }

        if(tacticContributionRequest.circuitBreakerEnabled) {
            tacticContributionItems.add(
                    TacticContributionItem.builder()
                            .label(((ArchitecturalPattern)resiliencyArchitecturalDecisionRepository.getCircuitBreakerPattern()).getName())
                            .build()
            );
        }

        if(tacticContributionRequest.basicAuthEnabled) {
            StringBuilder note = new StringBuilder();
            ArchitecturalTactic basicAuthenticationTactic = (ArchitecturalTactic)securityArchitecturalDecisionRepository.getBasicAuthTactic();
            Optional<QualityTradeoff> qualityTradeoff = basicAuthenticationTactic.getArchitecturalCharacteristic().getQualityTradeoffs().stream()
                            .filter(qt -> qt.getArchitecturalCharacteristic().getName().equals(ArchitecturalCharacteristics.SECURITY))
                                    .findFirst();
            if(qualityTradeoff.isPresent()) {
                note.append(((SecurityQualityTradeoff)qualityTradeoff.get()).getVulnerabilityPrevented());
            }
            tacticContributionItems.add(
                    TacticContributionItem.builder()
                            .label(((ArchitecturalTactic)securityArchitecturalDecisionRepository.getBasicAuthTactic()).getName())
                            .note(note.toString())
                            .build()
            );
        }

        if (tacticContributionRequest.timeoutEnabled) {
            tacticContributionItems.add(
                    TacticContributionItem.builder()
                            .label(((ArchitecturalPattern)resiliencyArchitecturalDecisionRepository.getTimeoutPattern()).getName()
                                    +"(" + tacticContributionRequest.timeoutMs + " ms)")
                            .build()
            );
        }
        return tacticContributionItems;
    }

    public List<TacticContributionItem> buildRetryContribution(TacticTCCContributionRequest tacticContributionRequest, int responseBytes) {
        List<TacticContributionItem> tacticContributionItems = new ArrayList<>();
        if(tacticContributionRequest.retryEnabled) {
            int retryExtra = (int) Math.round(tacticContributionRequest.baseRps * tacticContributionRequest.retryErrorRatePct / 100.0);
            double cost = getMonthlyEgressRPSDeltaCost(tacticContributionRequest.baseRps, retryExtra, responseBytes);
            TacticContributionItem item = TacticContributionItem.builder()
                    .label(((ArchitecturalPattern)resiliencyArchitecturalDecisionRepository.getRetryPattern()).getName())
                    .value(tacticContributionRequest.retryErrorRatePct + "% error rate")
                    .kind(RPS)
                    .rpsAdded(retryExtra)
                    .detail("+" + retryExtra + " req/s = " + tacticContributionRequest.retryErrorRatePct
                            + "% of " + tacticContributionRequest.baseRps + " base RPS")
                    .estimatedMonthlyCostUsd(round2(cost))
                    .costDisplayLabel(cost >= 0.005
                            ? "+$" + round2(cost) + "/mo"
                            : "< +$0.01/mo")
                    .build();
            tacticContributionItems.add(item);
        }
        return tacticContributionItems;
    }

    public List<TacticContributionItem> buildTlsContribution(TacticTCCContributionRequest tacticContributionRequest, int responseBytes) {
        List<TacticContributionItem> tacticContributionItems = new ArrayList<>();
        if(tacticContributionRequest.tlsEnabled || tacticContributionRequest.mtlsEnabled) {
            int  messagesPerReconnect = tacticContributionRequest.mtlsEnabled ? 5 : 2;
            int  handshakeRps = (int) Math.round((double) tacticContributionRequest.tlsReconnectsPerHour * messagesPerReconnect / 3600.0);
            int  tlsBytesOverhead = (tacticContributionRequest.tlsOverheadBytesFromBackend > 0)
                    ? tacticContributionRequest.tlsOverheadBytesFromBackend
                    : TLSOverhead.TLS_HANDSHAKE_MESSAGES.getOverhead();
            String byteSource = tacticContributionRequest.tlsOverheadBytesFromBackend > 0 ? ", from backend" : ", est. typical";
            String label      = tacticContributionRequest.mtlsEnabled ?
                    ((ArchitecturalTactic)securityArchitecturalDecisionRepository.getMTLSTactic()).getName()
                    : ((ArchitecturalTactic)securityArchitecturalDecisionRepository.getTLSTactic()).getName();
            double rpsCost   = handshakeRps > 0
                    ? getMonthlyEgressRPSDeltaCost(tacticContributionRequest.baseRps, handshakeRps, responseBytes) : 0;
            double byteCost  = getMonthlyEgressByteDeltaCost(tacticContributionRequest.baseRps, responseBytes, tlsBytesOverhead);
            double totalCost = rpsCost + byteCost;

            TacticContributionItem item = TacticContributionItem.builder()
                    .label(label)
                    .kind(handshakeRps > 0 ? RPS_AND_BYTES : BYTES)
                    .rpsAdded(handshakeRps)
                    .bytesAdded(tlsBytesOverhead)
                    .detail("+" + tlsBytesOverhead + " B/frame (RFC 8446" + byteSource + ")"
                            + (handshakeRps > 0 ? " \u00b7 +" + handshakeRps + " handshake req/s" : ""))
                    .estimatedMonthlyCostUsd(round2(totalCost))
                    .costDisplayLabel(totalCost >= 0.005
                            ? "+$" + round2(totalCost) + "/mo"
                            : "< +$0.01/mo")
                    .build();
            tacticContributionItems.add(item);
        }
        return tacticContributionItems;
    }

    private  double getMonthlyEgressRPSDeltaCost(int baseRps, int rpsAdded, int responseBytes) {
        return getNetworkingEgressCost(baseRps + rpsAdded, responseBytes)
                - getNetworkingEgressCost(baseRps, responseBytes);
    }

    private double getNetworkingEgressCost(int rps, int responseBytes) {

        List<Double> dataTransferRates = networkingCostCalculator.getDataTransferRates();

        double gbPerMonth = (double) rps * SECONDS_PER_MONTH * responseBytes / BYTES_PER_GB;
        double cost = 0;
        double remaining = gbPerMonth;

        if (remaining <= 0) return 0;

        double tier1Used = Math.min(remaining, networkingCostCalculator.getStandardThresholdLimits()[0]);
        cost += tier1Used * dataTransferRates.getFirst();
        remaining -= tier1Used;

        if (remaining > 0) {
            double tier2Used = Math.min(remaining, networkingCostCalculator.getStandardThresholdLimits()[1]);
            cost += tier2Used * dataTransferRates.get(1);
            remaining -= tier2Used;
        }
        if (remaining > 0) {
            double tier3Used = Math.min(remaining, networkingCostCalculator.getStandardThresholdLimits()[2]);
            cost += tier3Used * dataTransferRates.get(2);
            remaining -= tier3Used;
        }
        if (remaining > 0) {
            cost += remaining * dataTransferRates.getLast();
        }

        return cost;
    }


    private double getMonthlyEgressByteDeltaCost(int rps, int responseBytes, int extraBytes) {
        return getNetworkingEgressCost(rps, responseBytes + extraBytes)
                - getNetworkingEgressCost(rps, responseBytes);
    }

    public List<TacticContributionItem> buildOAuthContribution(TacticTCCContributionRequest tacticContributionRequest, int responseBytes) {
        List<TacticContributionItem> tacticContributionItems = new ArrayList<>();
        if(tacticContributionRequest.oauthEnabled) {
            int  ttl            = tacticContributionRequest.tokenTtlSeconds    > 0 ? tacticContributionRequest.tokenTtlSeconds    : 3600;
            int  clients        = tacticContributionRequest.concurrentClients  > 0 ? tacticContributionRequest.concurrentClients  : 1;
            int  tokenAcqRps    = (int) Math.round((double) tacticContributionRequest.baseRps / (ttl * clients));
            int  introspRps     = OAuthTokenValidationModes.REMOTE_INTROSPECTION.name().equals(tacticContributionRequest.tokenValidationMode)
                    ? tacticContributionRequest.baseRps : 0;
            int  totalRpsAdded  = tokenAcqRps + introspRps;
            int  jwtBytes       = (tacticContributionRequest.jwtOverheadBytesFromBackend > 0)
                    ? tacticContributionRequest.jwtOverheadBytesFromBackend
                    : JWTOverhead.JWT_OVERHEAD_BYTES_TYPICAL.getOverhead();
            String byteSource   = tacticContributionRequest.jwtOverheadBytesFromBackend > 0 ? ", from backend" : ", est. typical";
            String modeSuffix   = OAuthTokenValidationModes.REMOTE_INTROSPECTION.name().equals(tacticContributionRequest.tokenValidationMode)
                    ? " (remote)" : " (local)";

            double rpsCost      = totalRpsAdded > 0
                    ? getMonthlyEgressRPSDeltaCost(tacticContributionRequest.baseRps, totalRpsAdded, responseBytes) : 0;

            StringBuilder detailBuilder = new StringBuilder();
            detailBuilder.append("+").append(jwtBytes).append("B JWT header (RFC 7519")
                    .append(byteSource).append(")");
            if (tokenAcqRps > 0)
                detailBuilder.append(" \u00b7 +").append(tokenAcqRps).append(" token acq/s");
            if (introspRps > 0)
                detailBuilder.append(" \u00b7 +").append(introspRps).append(" intr/s");
            detailBuilder.append(" \u00b7 AWS inbound = $0");

            TacticContributionItem item = TacticContributionItem.builder()
                    .label(((ArchitecturalTactic)securityArchitecturalDecisionRepository.getOAuthTactic()).getName()
                            + modeSuffix)
                    .kind(RPS_AND_BYTES) // Both: rps and bytes
                    .rpsAdded(totalRpsAdded)
                    .bytesAdded(jwtBytes)
                    .detail(detailBuilder.toString())
                    .jwtOnRequestOnly(true)
                    .build();

            if (item.jwtOnRequestOnly && rpsCost == 0) {
                item.estimatedMonthlyCostUsd = 0;
                item.costDisplayLabel = (jwtBytes > 0 ? "+" + jwtBytes + " B/req inbound (free)\n" : "");
            } else {
                item.estimatedMonthlyCostUsd = round2(rpsCost);
                item.costDisplayLabel = rpsCost >= 0.005
                        ? "+$" + round2(rpsCost) + "/mo"
                        : "< +$0.01/mo";
            }
            tacticContributionItems.add(item);
        }
        return  tacticContributionItems;
    }
}

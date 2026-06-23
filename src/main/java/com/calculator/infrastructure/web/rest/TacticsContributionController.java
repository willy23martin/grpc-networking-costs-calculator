package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.networking.NetworkingCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.networking.aws.AWSDataTransferCostCalculationService;
import com.calculator.application.services.mapper.tradeoffs.security.SecurityTradeoffMapperService;
import com.calculator.domain.dto.responses.TacticContributionItem;
import com.calculator.domain.dto.requests.TacticContributionRequest;
import com.calculator.domain.dto.responses.TacticContributionResponse;
import com.calculator.domain.dto.tactics.security.tls.TLSOverhead;
import com.calculator.domain.model.architecture.tactics.security.JWTOverhead;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.calculator.application.services.calculators.cost.cloud.networking.aws.AWSDataTransferCostCalculationService.AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB;
import static com.calculator.application.services.utils.MathUtils.round2;
import static com.calculator.infrastructure.web.rest.TCOCalculatorController.BYTES_PER_GB;
import static com.calculator.infrastructure.web.rest.TCOCalculatorController.SECONDS_PER_MONTH;

@RestController
@RequestMapping("/api/cost")
@CrossOrigin(origins = "*")
public class TacticsContributionController {

    @Autowired
    SecurityTradeoffMapperService securityTradeoffMapperService;

    @Autowired
    NetworkingCostCalculator networkingCostCalculator;

    @PostMapping("/tactic-contributions")
    public ResponseEntity<TacticContributionResponse> calculateTacticContributions(
            @RequestBody TacticContributionRequest tacticContributionRequest) {

        TacticContributionResponse resp = new TacticContributionResponse();

        int placeholderBytes = 0;  // default proto response size placeholder
        int responseBytes    = tacticContributionRequest.protoResponseSizeEffectiveBytes > 0
                ? tacticContributionRequest.protoResponseSizeEffectiveBytes
                : placeholderBytes;
        resp.placeholderResponseBytes = placeholderBytes;
        resp.usedPlaceholderBytes = tacticContributionRequest.protoResponseSizeEffectiveBytes <= 0;

        // ── 1. Structural / informational tactics (no cost impact) ──────────
        addInfoTactic(resp, tacticContributionRequest.clientSideLoadBalancingEnabled,
                "Client-side Load Balancing", null, null);
        addInfoTactic(resp, tacticContributionRequest.serverSideLoadBalancingEnabled,
                "Server-side Load Balancing", null, null);
        addInfoTactic(resp, tacticContributionRequest.circuitBreakerEnabled,
                "Circuit Breaker", null, null);
        addInfoTactic(resp, tacticContributionRequest.basicAuthEnabled,
                "Basic Authentication", null,
                "\u26a0 Not recommended for production");
        if (tacticContributionRequest.timeoutEnabled) {
            addInfoTactic(resp, true,
                    "Timeout (" + tacticContributionRequest.timeoutMs + " ms)", null, null);
        }

        // ── 3. Retry ─────────────────────────────────────────────────────────
        buildRetryContribution(resp, tacticContributionRequest, responseBytes);

        // ── 4. TLS / mTLS ────────────────────────────────────────────────────
        buildTlsContribution(resp, tacticContributionRequest, responseBytes);

        // ── 5. OAuth 2.0 + JWT ───────────────────────────────────────────────
        buildOAuthContribution(resp, tacticContributionRequest, responseBytes);

        // ── Total networking delta ────────────────────────────────────────────
        double total = resp.contributions.stream()
                .mapToDouble(c -> c.estimatedMonthlyCostUsd)
                .filter(v -> v > 0)
                .sum();
        resp.totalTacticNetworkingDeltaUsd = round2(total);

        return ResponseEntity.ok(resp);
    }

    // ── Retry ────────────────────────────────────────────────────────────────
    private void buildRetryContribution(TacticContributionResponse resp,
                                        TacticContributionRequest req,
                                        int responseBytes) {
        if (!req.retryEnabled) return;

        int    retryExtra = (int) Math.round(req.baseRps * req.retryErrorRatePct / 100.0);
        double cost       = egressCostDeltaUsd(req.baseRps, retryExtra, responseBytes);

        TacticContributionItem item = new TacticContributionItem();
        item.label    = "Retry";
        item.value    = req.retryErrorRatePct + "% error rate";
        item.kind     = "rps";
        item.rpsAdded = retryExtra;
        item.bytesAdded = 0;
        item.detail   = "+" + retryExtra + " req/s = " + req.retryErrorRatePct
                + "% of " + req.baseRps + " base RPS";
        item.estimatedMonthlyCostUsd = round2(cost);
        item.costDisplayLabel = cost >= 0.005
                ? "+$" + round2(cost) + "/mo"
                : "< +$0.01/mo";

        resp.contributions.add(item);
    }

    // ── TLS / mTLS ───────────────────────────────────────────────────────────
    private void buildTlsContribution(TacticContributionResponse resp,
                                      TacticContributionRequest req,
                                      int responseBytes) {
        if (!req.tlsEnabled && !req.mtlsEnabled) return;

        int  messagesPerReconnect = req.mtlsEnabled ? 5 : 2;
        int  handshakeRps         = (int) Math.round(
                (double) req.tlsReconnectsPerHour * messagesPerReconnect / 3600.0);
        int  tlsBytesOverhead     = (req.tlsOverheadBytesFromBackend > 0)
                ? req.tlsOverheadBytesFromBackend
                : TLSOverhead.TLS_HANDSHAKE_MESSAGES.getOverhead();
        String byteSource = req.tlsOverheadBytesFromBackend > 0 ? ", from backend" : ", est. typical";
        String label      = req.mtlsEnabled ? "mTLS (mutual TLS)" : "TLS (one-way)";

        double rpsCost   = handshakeRps > 0
                ? egressCostDeltaUsd(req.baseRps, handshakeRps, responseBytes) : 0;
        double byteCost  = egressByteDeltaUsd(req.baseRps, responseBytes, tlsBytesOverhead);
        double totalCost = rpsCost + byteCost;

        TacticContributionItem item = new TacticContributionItem();
        item.label      = label;
        item.kind       = handshakeRps > 0 ? "both" : "bytes";
        item.rpsAdded   = handshakeRps;
        item.bytesAdded = tlsBytesOverhead;
        item.detail     = "+" + tlsBytesOverhead + " B/frame (RFC 8446" + byteSource + ")"
                + (handshakeRps > 0 ? " \u00b7 +" + handshakeRps + " handshake req/s" : "");
        item.estimatedMonthlyCostUsd = round2(totalCost);
        item.costDisplayLabel = totalCost >= 0.005
                ? "+$" + round2(totalCost) + "/mo"
                : "< +$0.01/mo";

        resp.contributions.add(item);
    }

    // ── OAuth 2.0 + JWT ──────────────────────────────────────────────────────
    private void buildOAuthContribution(TacticContributionResponse resp,
                                        TacticContributionRequest req,
                                        int responseBytes) {
        if (!req.oauthEnabled) return;

        int  ttl            = req.tokenTtlSeconds    > 0 ? req.tokenTtlSeconds    : 3600;
        int  clients        = req.concurrentClients  > 0 ? req.concurrentClients  : 1;
        int  tokenAcqRps    = (int) Math.round((double) req.baseRps / (ttl * clients));
        int  introspRps     = "REMOTE_INTROSPECTION".equals(req.tokenValidationMode)
                ? req.baseRps : 0;
        int  totalRpsAdded  = tokenAcqRps + introspRps;
        int  jwtBytes       = (req.jwtOverheadBytesFromBackend > 0)
                ? req.jwtOverheadBytesFromBackend
                : JWTOverhead.JWT_OVERHEAD_BYTES_TYPICAL.getOverhead();
        String byteSource   = req.jwtOverheadBytesFromBackend > 0 ? ", from backend" : ", est. typical";
        String modeSuffix   = "REMOTE_INTROSPECTION".equals(req.tokenValidationMode)
                ? " (remote)" : " (local)";

        // JWT travels request-side (inbound) — AWS inbound is free.
        // Cost only from token-acquisition and introspection outbound calls.
        double rpsCost      = totalRpsAdded > 0
                ? egressCostDeltaUsd(req.baseRps, totalRpsAdded, responseBytes) : 0;

        StringBuilder detailBuilder = new StringBuilder();
        detailBuilder.append("+").append(jwtBytes).append("B JWT header (RFC 7519")
                .append(byteSource).append(")");
        if (tokenAcqRps > 0)
            detailBuilder.append(" \u00b7 +").append(tokenAcqRps).append(" token acq/s");
        if (introspRps > 0)
            detailBuilder.append(" \u00b7 +").append(introspRps).append(" intr/s");
        detailBuilder.append(" \u00b7 AWS inbound = $0");

        TacticContributionItem item = new TacticContributionItem();
        item.label             = "OAuth 2.0 + JWT" + modeSuffix;
        item.kind              = "both";
        item.rpsAdded          = totalRpsAdded;
        item.bytesAdded        = jwtBytes;
        item.jwtOnRequestOnly  = true;
        item.detail            = detailBuilder.toString();

        if (item.jwtOnRequestOnly && rpsCost == 0) {
            item.estimatedMonthlyCostUsd = 0;
            item.costDisplayLabel = (jwtBytes > 0 ? "+" + jwtBytes + " B/req inbound (free)\n" : "");
        } else {
            item.estimatedMonthlyCostUsd = round2(rpsCost);
            item.costDisplayLabel = rpsCost >= 0.005
                    ? "+$" + round2(rpsCost) + "/mo"
                    : "< +$0.01/mo";
        }

        resp.contributions.add(item);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void addInfoTactic(TacticContributionResponse resp,
                                      boolean enabled,
                                      String label,
                                      String value,
                                      String note) {
        if (!enabled) return;
        TacticContributionItem item = new TacticContributionItem();
        item.label   = label;
        item.value   = value;
        item.note    = note;
        item.kind    = "info";
        item.rpsAdded = 0;
        item.bytesAdded = 0;
        item.estimatedMonthlyCostUsd = 0;
        item.costDisplayLabel = "\u2014";
        resp.contributions.add(item);
    }

    /**
     * Monthly egress cost delta from adding extra RPS at a fixed response size.
     * delta = cost(baseRps + rpsAdded, bytes) - cost(baseRps, bytes)
     */
    private  double egressCostDeltaUsd(int baseRps, int rpsAdded, int responseBytes) {
        return monthlyCostUsd(baseRps + rpsAdded, responseBytes)
                - monthlyCostUsd(baseRps, responseBytes);
    }

    /**
     * Monthly egress cost delta from adding extra bytes per response at a fixed RPS.
     * delta = cost(rps, bytes + extraBytes) - cost(rps, bytes)
     */
    private double egressByteDeltaUsd(int rps, int responseBytes, int extraBytes) {
        return monthlyCostUsd(rps, responseBytes + extraBytes)
                - monthlyCostUsd(rps, responseBytes);
    }

    /**
     * Monthly egress cost for the given RPS and response size using AWS tiered pricing.
     */
    private double monthlyCostUsd(int rps, int responseBytes) {

        List<Double> dataTransferRates = ((AWSDataTransferCostCalculationService) networkingCostCalculator).getDataTransferRates();

        double gbPerMonth = (double) rps * SECONDS_PER_MONTH * responseBytes / BYTES_PER_GB;
        double cost = 0;
        double remaining = gbPerMonth;

        if (remaining <= 0) return 0;

        double tier1Used = Math.min(remaining, AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[0]);
        cost += tier1Used * dataTransferRates.getFirst();
        remaining -= tier1Used;

        if (remaining > 0) {
            double tier2Used = Math.min(remaining, AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[1]);
            cost += tier2Used * dataTransferRates.get(1);
            remaining -= tier2Used;
        }
        if (remaining > 0) {
            double tier3Used = Math.min(remaining, AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[2]);
            cost += tier3Used * dataTransferRates.get(2);
            remaining -= tier3Used;
        }
        if (remaining > 0) {
            cost += remaining * dataTransferRates.getLast();
        }

        return cost;
    }
}
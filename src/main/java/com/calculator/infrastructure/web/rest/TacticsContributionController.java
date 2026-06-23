package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.security.SecurityTradeoffMapperService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.calculator.application.services.utils.MathUtils.round2;

@RestController
@RequestMapping("/api/cost")
@CrossOrigin(origins = "*")
public class TacticsContributionController {

    // ── AWS egress tiers ──────────────────────────────────────────────────────
    private static final double SECONDS_PER_MONTH        = 2_592_000.0;
    private static final double BYTES_PER_GB             = 1_073_741_824.0;
    private static final double TIER_1_LIMIT_GB          = 10_240.0;
    private static final double TIER_2_LIMIT_GB          = 40_960.0;
    private static final double TIER_3_LIMIT_GB          = 102_400.0;
    private static final double TIER_1_RATE              = 0.09;
    private static final double TIER_2_RATE              = 0.085;
    private static final double TIER_3_RATE              = 0.07;
    private static final double TIER_4_RATE              = 0.05;
    // ── RFC byte overhead constants ───────────────────────────────────────────
    private static final int RFC_8446_TLS_FRAME_BYTES    = 29;   // AES-GCM typical
    private static final int RFC_7519_JWT_HEADER_BYTES   = 650;  // typical bearer token

    /* ================================================================
       REQUEST DTO
    ================================================================ */
    public static class TacticContributionRequest {
        @JsonProperty public int     baseRps                       = 0;

        // Sizes from the last /calculateTCO response (0 = use placeholder)
        @JsonProperty public int     protoResponseSizeEffectiveBytes = 0;
        @JsonProperty public int     tlsOverheadBytesFromBackend     = 0;
        @JsonProperty public int     jwtOverheadBytesFromBackend     = 0;

        // Structural / no-cost tactics
        @JsonProperty public boolean clientSideLoadBalancingEnabled = false;
        @JsonProperty public boolean serverSideLoadBalancingEnabled = false;
        @JsonProperty public boolean circuitBreakerEnabled          = false;
        @JsonProperty public boolean basicAuthEnabled               = false;
        @JsonProperty public boolean timeoutEnabled                 = false;
        @JsonProperty public int     timeoutMs                      = 0;

        // Retry
        @JsonProperty public boolean retryEnabled                  = false;
        @JsonProperty public double  retryErrorRatePct             = 5.0;

        // TLS / mTLS
        @JsonProperty public boolean tlsEnabled                    = false;
        @JsonProperty public boolean mtlsEnabled                   = false;
        @JsonProperty public int     tlsReconnectsPerHour          = 0;

        // OAuth 2.0 + JWT
        @JsonProperty public boolean oauthEnabled                  = false;
        @JsonProperty public String  tokenValidationMode           = "LOCAL";
        @JsonProperty public int     tokenTtlSeconds               = 3600;
        @JsonProperty public int     concurrentClients             = 1;
    }

    /* ================================================================
       RESPONSE DTO
    ================================================================ */
    public static class TacticContributionItem {
        @JsonProperty public String  label;
        @JsonProperty public String  value;          // optional sub-label, e.g. "5% error rate"
        @JsonProperty public String  kind;           // "info" | "rps" | "bytes" | "both"
        @JsonProperty public String  detail;         // one-line formula summary for the UI
        @JsonProperty public String  note;           // optional warning note
        @JsonProperty public int     rpsAdded;
        @JsonProperty public int     bytesAdded;
        @JsonProperty public boolean jwtOnRequestOnly;
        @JsonProperty public double  estimatedMonthlyCostUsd;  // 0 for kind="info"
        @JsonProperty public String  costDisplayLabel;         // formatted string for UI
    }

    public static class TacticContributionResponse {
        @JsonProperty public List<TacticContributionItem> contributions = new ArrayList<>();
        @JsonProperty public double  totalTacticNetworkingDeltaUsd;  // sum of networking cost increases
        @JsonProperty public int     placeholderResponseBytes;        // what was used when proto not available
        @JsonProperty public boolean usedPlaceholderBytes;
    }

    @Autowired
    SecurityTradeoffMapperService securityTradeoffMapperService;

    @PostMapping("/tactic-contributions")
    public ResponseEntity<TacticContributionResponse> calculateTacticContributions(
            @RequestBody TacticContributionRequest req) {

        TacticContributionResponse resp = new TacticContributionResponse();

        int    placeholderBytes = 1_200;  // default proto response size placeholder
        int    responseBytes    = req.protoResponseSizeEffectiveBytes > 0
                ? req.protoResponseSizeEffectiveBytes
                : placeholderBytes;
        resp.placeholderResponseBytes = placeholderBytes;
        resp.usedPlaceholderBytes     = req.protoResponseSizeEffectiveBytes <= 0;

        // ── 1. Structural / informational tactics (no cost impact) ──────────
        addInfoTactic(resp, req.clientSideLoadBalancingEnabled,
                "Client-side Load Balancing", null, null);
        addInfoTactic(resp, req.serverSideLoadBalancingEnabled,
                "Server-side Load Balancing", null, null);
        addInfoTactic(resp, req.circuitBreakerEnabled,
                "Circuit Breaker", null, null);
        addInfoTactic(resp, req.basicAuthEnabled,
                "Basic Authentication", null,
                "\u26a0 Not recommended for production");
        if (req.timeoutEnabled) {
            addInfoTactic(resp, true,
                    "Timeout (" + req.timeoutMs + " ms)", null, null);
        }

        // ── 3. Retry ─────────────────────────────────────────────────────────
        buildRetryContribution(resp, req, responseBytes);

        // ── 4. TLS / mTLS ────────────────────────────────────────────────────
        buildTlsContribution(resp, req, responseBytes);

        // ── 5. OAuth 2.0 + JWT ───────────────────────────────────────────────
        buildOAuthContribution(resp, req, responseBytes);

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
                : RFC_8446_TLS_FRAME_BYTES;
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
                : RFC_7519_JWT_HEADER_BYTES;
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
    private static void addInfoTactic(TacticContributionResponse resp,
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
    private static double egressCostDeltaUsd(int baseRps, int rpsAdded, int responseBytes) {
        return monthlyCostUsd(baseRps + rpsAdded, responseBytes)
                - monthlyCostUsd(baseRps, responseBytes);
    }

    /**
     * Monthly egress cost delta from adding extra bytes per response at a fixed RPS.
     * delta = cost(rps, bytes + extraBytes) - cost(rps, bytes)
     */
    private static double egressByteDeltaUsd(int rps, int responseBytes, int extraBytes) {
        return monthlyCostUsd(rps, responseBytes + extraBytes)
                - monthlyCostUsd(rps, responseBytes);
    }

    /**
     * Monthly egress cost for the given RPS and response size using AWS tiered pricing.
     */
    private static double monthlyCostUsd(int rps, int responseBytes) {
        double gbPerMonth = (double) rps * SECONDS_PER_MONTH * responseBytes / BYTES_PER_GB;
        double cost = 0;
        double remaining = gbPerMonth;

        if (remaining <= 0) return 0;

        double tier1Used = Math.min(remaining, TIER_1_LIMIT_GB);
        cost += tier1Used * TIER_1_RATE;
        remaining -= tier1Used;

        if (remaining > 0) {
            double tier2Used = Math.min(remaining, TIER_2_LIMIT_GB);
            cost += tier2Used * TIER_2_RATE;
            remaining -= tier2Used;
        }
        if (remaining > 0) {
            double tier3Used = Math.min(remaining, TIER_3_LIMIT_GB);
            cost += tier3Used * TIER_3_RATE;
            remaining -= tier3Used;
        }
        if (remaining > 0) {
            cost += remaining * TIER_4_RATE;
        }

        return cost;
    }
}
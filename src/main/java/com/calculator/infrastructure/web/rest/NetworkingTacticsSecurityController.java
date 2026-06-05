package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * NetworkingTacticsSecurityController
 *
 * Provides:
 *   GET  /api/security/tactic-mappings  → OWASP/CWE/ISO metadata per tactic
 *   POST /api/tco/effective-rps         → Computes effective RPS from active tactics
 *
 * RPS formula (all additive on top of baseRps, applied in order):
 *   1. SAGA  — multiplicative only when steps exit the VPC
 *              effectiveRps = baseRps × steps   (if sagaExternalVpc = true)
 *   2. Retry — additive, always applied to BASE RPS (not SAGA-multiplied RPS)
 *              extra = round(baseRps × errorPct / 100)
 *   3. TLS/mTLS handshakes — extra = round(reconnectsPerHour × msgs / 3600)
 *              msgs = 5 for mTLS, 2 for one-way TLS
 *   4. OAuth token acquisition — extra = round(baseRps / (ttlSeconds × clients))
 *      OAuth remote introspection — extra += baseRps  (if mode = REMOTE_INTROSPECTION)
 */
@RestController
@CrossOrigin(origins = "*")
public class NetworkingTacticsSecurityController {

    /* ================================================================
       POST /api/tco/effective-rps — REQUEST DTO
    ================================================================ */
    public static class EffectiveRpsRequest {
        @JsonProperty public int     baseRps                 = 0;

        // SAGA
        @JsonProperty public boolean sagaEnabled             = false;
        @JsonProperty public boolean sagaExternalVpc         = false;
        @JsonProperty public int     sagaCompensatable        = 0;
        @JsonProperty public int     sagaRetriable            = 0;
        @JsonProperty public int     sagaPivot                = 0;

        // Retry
        @JsonProperty public boolean retryEnabled            = false;
        @JsonProperty public double  retryErrorPct           = 5.0;

        // TLS / mTLS
        @JsonProperty public boolean tlsEnabled              = false;
        @JsonProperty public boolean mtlsEnabled             = false;
        @JsonProperty public int     tlsReconnectsPerHour    = 0;

        // OAuth 2.0 + JWT
        @JsonProperty public boolean oauthEnabled            = false;
        @JsonProperty public String  tokenValidationMode     = "LOCAL";  // LOCAL | REMOTE_INTROSPECTION
        @JsonProperty public int     tokenTtlSeconds         = 3600;
        @JsonProperty public int     concurrentClients       = 1;
    }

    /* ================================================================
       POST /api/tco/effective-rps — RESPONSE DTO
    ================================================================ */
    public static class EffectiveRpsResponse {
        @JsonProperty public int     baseRps;
        @JsonProperty public int     effectiveRps;
        @JsonProperty public boolean rpsWasAdjusted;
        @JsonProperty public List<String> breakdown = new ArrayList<>();

        // Per-tactic preview strings (used by the UI to update live previews)
        @JsonProperty public String  sagaPreview;
        @JsonProperty public String  oauthPreview;
        @JsonProperty public int     retryExtra;
        @JsonProperty public int     handshakeExtra;
        @JsonProperty public int     tokenAcqExtra;
        @JsonProperty public int     introspectionExtra;
    }

    /* ================================================================
       ENDPOINT
    ================================================================ */
    @PostMapping("/api/tco/effective-rps")
    public ResponseEntity<EffectiveRpsResponse> calculateEffectiveRps(
            @RequestBody EffectiveRpsRequest req) {

        EffectiveRpsResponse resp = new EffectiveRpsResponse();
        resp.baseRps = req.baseRps;

        if (req.baseRps <= 0) {
            resp.effectiveRps   = 0;
            resp.rpsWasAdjusted = false;
            return ResponseEntity.ok(resp);
        }

        int effective = req.baseRps;

        // ── 1. SAGA ──────────────────────────────────────────────────────────
        if (req.sagaEnabled) {
            int steps = req.sagaCompensatable + req.sagaRetriable + req.sagaPivot;
            if (steps > 0) {
                if (req.sagaExternalVpc) {
                    effective = req.baseRps * steps;
                    resp.breakdown.add("\u00d7" + steps + " SAGA (exits VPC → egress billed) = "
                            + effective + " req/s");
                    resp.sagaPreview = req.baseRps + " \u00d7 " + steps + " steps = "
                            + effective + " billable egress calls/s (\u26a0 exits VPC)";
                } else {
                    resp.breakdown.add(steps + " SAGA steps (intra-VPC, egress FREE — not added to billable RPS)");
                    resp.sagaPreview = req.baseRps + " \u00d7 " + steps
                            + " steps — all intra-VPC, $0.00/GB same-AZ. No egress charge.";
                }
            }
        }

        // ── 2. Retry (always uses BASE RPS, never SAGA-multiplied) ───────────
        if (req.retryEnabled) {
            int extra = (int) Math.round(req.baseRps * req.retryErrorPct / 100.0);
            effective += extra;
            resp.retryExtra = extra;
            resp.breakdown.add("+" + extra + " Retry (" + req.retryErrorPct
                    + "% \u00d7 " + req.baseRps + " base RPS = " + extra + " retry/s)");
        }

        // ── 3. TLS / mTLS handshakes ─────────────────────────────────────────
        if (req.tlsEnabled || req.mtlsEnabled) {
            int msgs         = req.mtlsEnabled ? 5 : 2;
            int handshakeRps = (int) Math.round(req.tlsReconnectsPerHour * msgs / 3600.0);
            if (handshakeRps > 0) {
                effective += handshakeRps;
                resp.handshakeExtra = handshakeRps;
                resp.breakdown.add("+" + handshakeRps + " "
                        + (req.mtlsEnabled ? "mTLS" : "TLS") + " handshakes");
            }
        }

        // ── 4. OAuth 2.0 token acquisition / remote introspection ────────────
        if (req.oauthEnabled) {
            int ttl     = req.tokenTtlSeconds  > 0 ? req.tokenTtlSeconds  : 3600;
            int clients = req.concurrentClients > 0 ? req.concurrentClients : 1;

            int tokenAcq   = (int) Math.round((double) req.baseRps / (ttl * clients));
            int introspRps = "REMOTE_INTROSPECTION".equals(req.tokenValidationMode) ? req.baseRps : 0;

            effective += tokenAcq + introspRps;
            resp.tokenAcqExtra        = tokenAcq;
            resp.introspectionExtra   = introspRps;

            if (tokenAcq > 0)   resp.breakdown.add("+" + tokenAcq + " token acq/s");
            if (introspRps > 0) resp.breakdown.add("+" + introspRps + " remote introspection/s");

            resp.oauthPreview = req.baseRps + " base"
                    + (introspRps > 0 ? " + " + introspRps + " intr" : "")
                    + (tokenAcq  > 0 ? " + " + tokenAcq  + " acq"  : "")
                    + " = " + effective + " req/s";
        }

        resp.effectiveRps   = effective;
        resp.rpsWasAdjusted = (effective != req.baseRps);
        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       GET /api/security/tactic-mappings — (unchanged, preserved below)
    ================================================================ */
    @GetMapping("/api/security/tactic-mappings")
    public ResponseEntity<List<TacticSecurityMapping>> getTacticSecurityMappings() {
        List<TacticSecurityMapping> mappings = new ArrayList<>();

        mappings.add(build("tactic-client-lb","Client-side Load Balancing","Reliability",
                new String[]{"A05:2021"},new String[]{"Security Misconfiguration"},
                new String[]{"CWE-693"},new String[]{"Availability","Fault Tolerance"},
                "Prevents single-node overload that can be exploited via resource-exhaustion attacks.",
                "No direct cloud cost impact."));

        mappings.add(build("tactic-server-lb","Server-side Load Balancing (ALB)","Reliability",
                new String[]{"A05:2021","A09:2021"},
                new String[]{"Security Misconfiguration","Security Logging and Monitoring Failures"},
                new String[]{"CWE-400","CWE-693"},new String[]{"Availability","Fault Tolerance"},
                "ALB terminates TLS at the edge and provides DDoS absorption.",
                "Adds ALB fixed hourly charge + LCU costs from AWS Pricing API."));

        mappings.add(build("tactic-timeout","Timeout","Resiliency",
                new String[]{"A04:2021"},new String[]{"Insecure Design"},
                new String[]{"CWE-400","CWE-770"},new String[]{"Time-behaviour","Fault Tolerance"},
                "Prevents slow-loris and resource-starvation attacks.",
                "No direct cloud cost impact."));

        mappings.add(build("tactic-retry","Retry","Resiliency",
                new String[]{"A04:2021"},new String[]{"Insecure Design"},
                new String[]{"CWE-400"},new String[]{"Fault Tolerance","Recoverability"},
                "Retry with exponential backoff prevents retry storms.",
                "Increases effective RPS proportional to the error rate configured."));

        mappings.add(build("tactic-cb","Circuit Breaker","Resiliency",
                new String[]{"A04:2021","A05:2021"},
                new String[]{"Insecure Design","Security Misconfiguration"},
                new String[]{"CWE-400","CWE-703"},new String[]{"Fault Tolerance","Recoverability"},
                "Prevents cascading failures from propagating across service boundaries.",
                "No direct cloud cost impact."));

        mappings.add(build("tactic-saga","SAGA Pattern","Microservices",
                new String[]{"A04:2021","A09:2021"},
                new String[]{"Insecure Design","Security Logging and Monitoring Failures"},
                new String[]{"CWE-362","CWE-841"},new String[]{"Integrity","Fault Tolerance"},
                "SAGA compensating transactions prevent partial-write inconsistency.",
                "Extra egress cost = base RPS × steps when steps exit the VPC."));

        mappings.add(build("tactic-apigw","API Gateway","Microservices",
                new String[]{"A01:2021","A03:2021","A04:2021","A07:2021"},
                new String[]{"Broken Access Control","Injection","Insecure Design","Identification and Authentication Failures"},
                new String[]{"CWE-284","CWE-89","CWE-306"},
                new String[]{"Confidentiality","Integrity","Non-Repudiation"},
                "API Gateway enforces rate limiting, WAF, auth token validation.",
                "Charged per million API calls + optional caching hourly rate."));

        mappings.add(build("tactic-tls","TLS (One-way)","Security",
                new String[]{"A02:2021","A07:2021"},
                new String[]{"Cryptographic Failures","Identification and Authentication Failures"},
                new String[]{"CWE-319","CWE-523","CWE-326"},
                new String[]{"Confidentiality","Integrity"},
                "TLS 1.3 (RFC 8446) encrypts the gRPC channel in transit.",
                "Adds ~29 B/frame TLS overhead. Reconnect RPS adds handshake requests."));

        mappings.add(build("tactic-mtls","mTLS (Mutual TLS)","Security",
                new String[]{"A01:2021","A02:2021","A07:2021"},
                new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                new String[]{"CWE-319","CWE-295","CWE-287","CWE-306"},
                new String[]{"Confidentiality","Integrity","Authenticity"},
                "mTLS enforces bidirectional certificate authentication.",
                "5 TLS messages per handshake vs 2 for one-way TLS."));

        mappings.add(build("tactic-oauth","OAuth 2.0 + JWT","Security",
                new String[]{"A01:2021","A02:2021","A07:2021"},
                new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                new String[]{"CWE-284","CWE-287","CWE-345","CWE-613"},
                new String[]{"Confidentiality","Accountability","Authenticity","Non-Repudiation"},
                "OAuth 2.0 + JWT enforce fine-grained access control per scope.",
                "JWT adds ~650 B per request (inbound, free). Token-acquisition RPS = baseRPS / (TTL × clients)."));

        mappings.add(build("tactic-basic-auth","Basic Authentication","Security",
                new String[]{"A02:2021","A07:2021"},
                new String[]{"Cryptographic Failures","Identification and Authentication Failures"},
                new String[]{"CWE-256","CWE-522","CWE-287"},new String[]{"Confidentiality"},
                "⚠ NOT RECOMMENDED FOR PRODUCTION. No token rotation or scope-based access control.",
                "No byte or RPS overhead."));

        mappings.add(build("sec-guardduty","Amazon GuardDuty","Cloud Security",
                new String[]{"A09:2021"},new String[]{"Security Logging and Monitoring Failures"},
                new String[]{"CWE-223","CWE-778"},new String[]{"Accountability","Non-Repudiation"},
                "Continuous ML-based threat detection of VPC Flow Logs, DNS logs, and CloudTrail events.",
                "Per GB of logs analysed. First 500 GB/mo free."));

        mappings.add(build("sec-inspector","Amazon Inspector","Cloud Security",
                new String[]{"A06:2021"},new String[]{"Vulnerable and Outdated Components"},
                new String[]{"CWE-1104","CWE-937"},new String[]{"Integrity"},
                "Automated CVE scanning of EC2 instance OS packages and container images in ECR.",
                "Per EC2 instance per month."));

        mappings.add(build("sec-waf","AWS WAF","Cloud Security",
                new String[]{"A03:2021","A04:2021"},new String[]{"Injection","Insecure Design"},
                new String[]{"CWE-89","CWE-79","CWE-20","CWE-400"},new String[]{"Integrity","Confidentiality"},
                "Layer-7 firewall blocking SQL injection, XSS, and rate-based DDoS.",
                "WebACL fee + per-rule + per-million-request charges."));

        mappings.add(build("sec-macie","Amazon Macie","Cloud Security",
                new String[]{"A02:2021"},new String[]{"Cryptographic Failures"},
                new String[]{"CWE-312","CWE-313","CWE-359"},new String[]{"Confidentiality"},
                "ML-powered PII and credentials discovery in S3 buckets.",
                "Per GB of S3 data classified. First 1 GB/mo free."));

        mappings.add(build("sec-cloudwatch","CloudWatch Logs","Cloud Security",
                new String[]{"A09:2021"},new String[]{"Security Logging and Monitoring Failures"},
                new String[]{"CWE-223","CWE-778","CWE-779"},new String[]{"Accountability","Non-Repudiation"},
                "Centralised log ingestion, storage, and metric alarms.",
                "Per GB ingested + per GB-month stored."));

        mappings.add(build("sec-audit","AWS Audit Manager","Cloud Security",
                new String[]{"A09:2021","A05:2021"},
                new String[]{"Security Logging and Monitoring Failures","Security Misconfiguration"},
                new String[]{"CWE-778","CWE-693"},new String[]{"Accountability","Non-Repudiation"},
                "Continuous evidence collection for SOC 2, PCI DSS, ISO 27001, and HIPAA controls.",
                "Per active assessment per month."));

        mappings.add(build("sec-kms","AWS KMS (Encryption)","Cloud Security",
                new String[]{"A02:2021"},new String[]{"Cryptographic Failures"},
                new String[]{"CWE-311","CWE-312","CWE-326","CWE-330"},
                new String[]{"Confidentiality","Integrity"},
                "Customer-managed keys for envelope encryption of S3, RDS, EBS, and Secrets Manager.",
                "$1.00/CMK/month + $0.03 per 10,000 API calls."));

        return ResponseEntity.ok(mappings);
    }

    /* ================================================================
       HELPER FACTORY
    ================================================================ */
    public static class TacticSecurityMapping {
        @JsonProperty public String   tacticId;
        @JsonProperty public String   tacticName;
        @JsonProperty public String   tacticCategory;
        @JsonProperty public String[] owaspTop10;
        @JsonProperty public String[] owaspLabels;
        @JsonProperty public String[] cweIds;
        @JsonProperty public String[] iso25010Attributes;
        @JsonProperty public String   vulnerabilityPrevented;
        @JsonProperty public String   securityRationale;
        @JsonProperty public String   costImpactNote;
    }

    private static TacticSecurityMapping build(
            String id, String name, String category,
            String[] owasp, String[] owaspLabels,
            String[] cwe, String[] iso,
            String vuln, String cost) {
        TacticSecurityMapping m = new TacticSecurityMapping();
        m.tacticId               = id;
        m.tacticName             = name;
        m.tacticCategory         = category;
        m.owaspTop10             = owasp;
        m.owaspLabels            = owaspLabels;
        m.cweIds                 = cwe;
        m.iso25010Attributes     = iso;
        m.vulnerabilityPrevented = vuln;
        m.securityRationale      = vuln;
        m.costImpactNote         = cost;
        return m;
    }
}
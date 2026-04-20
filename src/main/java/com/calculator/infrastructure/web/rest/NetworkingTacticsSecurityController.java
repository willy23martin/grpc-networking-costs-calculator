package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * NetworkingTacticsSecurityController
 *
 * Provides static security metadata for every architecture tactic and cloud
 * security service modelled in the gRPC MACH TCO Calculator.
 *
 * Each tactic entry includes:
 *  - The OWASP Top 10:2021 categories it mitigates
 *  - The vulnerability class it prevents (with CWE reference)
 *  - The ISO/IEC 25010:2023 quality-attribute it addresses
 *  - A short architect-facing description of the security rationale
 *
 * Sources:
 *  - OWASP Top 10:2021  https://owasp.org/Top10/
 *  - CWE MITRE          https://cwe.mitre.org/
 *  - ISO/IEC 25010:2023 (Security sub-characteristics: Confidentiality,
 *      Integrity, Non-Repudiation, Accountability, Authenticity)
 *  - RFC 8446 (TLS 1.3), RFC 7519 (JWT), RFC 6749 (OAuth 2.0)
 *
 * Endpoint:
 *  GET /api/security/tactic-mappings  → all tactic → OWASP/CWE/ISO mappings
 */
@RestController
@RequestMapping("/api/security")
@CrossOrigin(origins = "*")
public class NetworkingTacticsSecurityController {

    /* ================================================================
       RESPONSE DTO
    ================================================================ */
    public static class TacticSecurityMapping {
        @JsonProperty public String   tacticId;
        @JsonProperty public String   tacticName;
        @JsonProperty public String   tacticCategory;       // Reliability / Resiliency / Security / Cloud
        @JsonProperty public String[] owaspTop10;           // e.g. ["A02:2021", "A07:2021"]
        @JsonProperty public String[] owaspLabels;          // human-readable OWASP labels
        @JsonProperty public String[] cweIds;               // e.g. ["CWE-319", "CWE-523"]
        @JsonProperty public String[] iso25010Attributes;   // ISO/IEC 25010:2023 sub-characteristics
        @JsonProperty public String   vulnerabilityPrevented;
        @JsonProperty public String   securityRationale;
        @JsonProperty public String   costImpactNote;
    }

    /* ================================================================
       ENDPOINT
    ================================================================ */
    @GetMapping("/tactic-mappings")
    public ResponseEntity<List<TacticSecurityMapping>> getTacticSecurityMappings() {
        List<TacticSecurityMapping> mappings = new ArrayList<>();

        // ── RELIABILITY TACTICS ─────────────────────────────────────────────

        mappings.add(build(
                "tactic-client-lb", "Client-side Load Balancing", "Reliability",
                new String[]{"A05:2021"},
                new String[]{"Security Misconfiguration"},
                new String[]{"CWE-693"},
                new String[]{"Availability", "Fault Tolerance"},
                "Prevents single-node overload that can be exploited via resource-exhaustion attacks. " +
                        "Distributes load so no single service instance becomes a denial-of-service target.",
                "No direct cloud cost impact."
        ));

        mappings.add(build(
                "tactic-server-lb", "Server-side Load Balancing (ALB)", "Reliability",
                new String[]{"A05:2021", "A09:2021"},
                new String[]{"Security Misconfiguration", "Security Logging and Monitoring Failures"},
                new String[]{"CWE-400", "CWE-693"},
                new String[]{"Availability", "Fault Tolerance"},
                "ALB terminates TLS at the edge and provides DDoS absorption (AWS Shield Standard included). " +
                        "Centralises access logging, enabling detection of abnormal traffic patterns per " +
                        "OWASP A09 monitoring requirements.",
                "Adds ALB fixed hourly charge + LCU costs from AWS Pricing API."
        ));

        // ── RESILIENCY TACTICS ───────────────────────────────────────────────

        mappings.add(build(
                "tactic-timeout", "Timeout", "Resiliency",
                new String[]{"A04:2021"},
                new String[]{"Insecure Design"},
                new String[]{"CWE-400", "CWE-770"},
                new String[]{"Time-behaviour", "Fault Tolerance"},
                "Prevents slow-loris and resource-starvation attacks where a malicious client holds " +
                        "connections open indefinitely. Enforcing timeouts bounds resource consumption per " +
                        "OWASP A04 insecure design protection.",
                "No direct cloud cost impact."
        ));

        mappings.add(build(
                "tactic-retry", "Retry", "Resiliency",
                new String[]{"A04:2021"},
                new String[]{"Insecure Design"},
                new String[]{"CWE-400"},
                new String[]{"Fault Tolerance", "Recoverability"},
                "Retry with exponential backoff and jitter prevents retry storms that can amplify a " +
                        "transient availability incident into a sustained outage. Must be bounded to avoid " +
                        "amplifying DDoS traffic.",
                "Increases effective RPS proportional to the error rate configured."
        ));

        mappings.add(build(
                "tactic-cb", "Circuit Breaker", "Resiliency",
                new String[]{"A04:2021", "A05:2021"},
                new String[]{"Insecure Design", "Security Misconfiguration"},
                new String[]{"CWE-400", "CWE-703"},
                new String[]{"Fault Tolerance", "Recoverability"},
                "Prevents cascading failures from propagating across service boundaries — a common " +
                        "amplifier of availability attacks. Open circuit stops flooding unhealthy downstream " +
                        "services, limiting blast radius per OWASP A04.",
                "No direct cloud cost impact."
        ));

        // ── MICROSERVICES PATTERNS ───────────────────────────────────────────

        mappings.add(build(
                "tactic-saga", "SAGA Pattern", "Microservices",
                new String[]{"A04:2021", "A09:2021"},
                new String[]{"Insecure Design", "Security Logging and Monitoring Failures"},
                new String[]{"CWE-362", "CWE-841"},
                new String[]{"Integrity", "Fault Tolerance"},
                "SAGA compensating transactions prevent partial-write inconsistency that attackers can " +
                        "exploit (e.g., charging a card but not granting access). Each step must be auditable " +
                        "per OWASP A09 to detect mid-saga injection or replay attacks.",
                "Extra egress cost = base RPS × (steps − 1) when steps exit the VPC."
        ));

        mappings.add(build(
                "tactic-apigw", "API Gateway", "Microservices",
                new String[]{"A01:2021", "A03:2021", "A04:2021", "A07:2021"},
                new String[]{"Broken Access Control", "Injection", "Insecure Design",
                        "Identification and Authentication Failures"},
                new String[]{"CWE-284", "CWE-89", "CWE-306"},
                new String[]{"Confidentiality", "Integrity", "Non-Repudiation"},
                "API Gateway is the single entry point that enforces rate limiting (A04), blocks " +
                        "injection attacks via WAF integration (A03), validates auth tokens before forwarding " +
                        "requests (A07), and applies resource-based access policies (A01). Centralises " +
                        "audit trails for all inbound requests.",
                "Charged per million API calls + optional caching hourly rate."
        ));

        // ── SECURITY TACTICS ─────────────────────────────────────────────────

        mappings.add(build(
                "tactic-tls", "TLS (One-way)", "Security",
                new String[]{"A02:2021", "A07:2021"},
                new String[]{"Cryptographic Failures", "Identification and Authentication Failures"},
                new String[]{"CWE-319", "CWE-523", "CWE-326"},
                new String[]{"Confidentiality", "Integrity"},
                "TLS 1.3 (RFC 8446) encrypts the gRPC channel in transit, preventing eavesdropping " +
                        "(OWASP A02 — Cryptographic Failures: CWE-319 Cleartext Transmission). " +
                        "Server certificate validates service identity, preventing man-in-the-middle attacks " +
                        "(OWASP A07 — CWE-523 Unprotected Transport of Credentials).",
                "Adds ~29 B/frame TLS overhead counted in egress. Reconnect RPS adds handshake requests."
        ));

        mappings.add(build(
                "tactic-mtls", "mTLS (Mutual TLS)", "Security",
                new String[]{"A01:2021", "A02:2021", "A07:2021"},
                new String[]{"Broken Access Control", "Cryptographic Failures",
                        "Identification and Authentication Failures"},
                new String[]{"CWE-319", "CWE-295", "CWE-287", "CWE-306"},
                new String[]{"Confidentiality", "Integrity", "Authenticity"},
                "mTLS enforces bidirectional certificate authentication: only services holding a " +
                        "valid certificate can connect (OWASP A01 — CWE-306 Missing Authentication; " +
                        "OWASP A07 — CWE-287 Improper Authentication). Prevents rogue service injection " +
                        "and lateral movement between microservices. Required for zero-trust architectures.",
                "5 TLS messages per handshake vs 2 for one-way TLS. Adds reconnect RPS."
        ));

        mappings.add(build(
                "tactic-oauth", "OAuth 2.0 + JWT", "Security",
                new String[]{"A01:2021", "A02:2021", "A07:2021"},
                new String[]{"Broken Access Control", "Cryptographic Failures",
                        "Identification and Authentication Failures"},
                new String[]{"CWE-284", "CWE-287", "CWE-345", "CWE-613"},
                new String[]{"Confidentiality", "Accountability", "Authenticity", "Non-Repudiation"},
                "OAuth 2.0 delegated authorisation (RFC 6749) + JWT bearer tokens (RFC 7519) " +
                        "enforce fine-grained access control per scope claim (OWASP A01 — CWE-284). " +
                        "Signed JWTs prevent token forgery (OWASP A02 — CWE-345 Insufficient Verification " +
                        "of Data Authenticity). Short-lived tokens bound exposure of stolen credentials " +
                        "(OWASP A07 — CWE-613 Insufficient Session Expiration).",
                "JWT adds ~650 B per request (inbound, AWS charges $0). " +
                        "Token-acquisition RPS = baseRPS / (TTL × concurrentClients)."
        ));

        mappings.add(build(
                "tactic-basic-auth", "Basic Authentication", "Security",
                new String[]{"A02:2021", "A07:2021"},
                new String[]{"Cryptographic Failures", "Identification and Authentication Failures"},
                new String[]{"CWE-256", "CWE-522", "CWE-287"},
                new String[]{"Confidentiality"},
                "⚠ NOT RECOMMENDED FOR PRODUCTION. Basic Auth encodes credentials in Base64 " +
                        "(not encrypted) — vulnerable to credential sniffing if TLS is not enforced " +
                        "(OWASP A02 — CWE-256 Unprotected Storage of Credentials). No token rotation, " +
                        "no scope-based access control. Use OAuth 2.0 + mTLS instead for gRPC services.",
                "No byte or RPS overhead (credentials in metadata, negligible size)."
        ));

        // ── CLOUD SECURITY SERVICES ──────────────────────────────────────────

        mappings.add(build(
                "sec-guardduty", "Amazon GuardDuty", "Cloud Security",
                new String[]{"A09:2021"},
                new String[]{"Security Logging and Monitoring Failures"},
                new String[]{"CWE-223", "CWE-778"},
                new String[]{"Accountability", "Non-Repudiation"},
                "Continuous ML-based threat detection of VPC Flow Logs, DNS logs, and CloudTrail " +
                        "events. Detects: credential exfiltration (compromised EC2 instance communicating " +
                        "with known C&C IPs), reconnaissance (unusual API enumeration), and data exfiltration " +
                        "(unusual S3 access). Directly addresses OWASP A09 — Security Logging and Monitoring " +
                        "Failures (CWE-223 Omission of Security-relevant Information).",
                "Per GB of logs analysed. First 500 GB/mo free."
        ));

        mappings.add(build(
                "sec-inspector", "Amazon Inspector", "Cloud Security",
                new String[]{"A06:2021"},
                new String[]{"Vulnerable and Outdated Components"},
                new String[]{"CWE-1104", "CWE-937"},
                new String[]{"Integrity"},
                "Automated CVE scanning of EC2 instance OS packages and container images in ECR. " +
                        "Directly addresses OWASP A06 — Vulnerable and Outdated Components " +
                        "(CWE-1104 Use of Unmaintained Third Party Components). Provides a continuous " +
                        "vulnerability risk score so unpatched CVEs in gRPC runtime dependencies are " +
                        "detected before exploitation.",
                "Per EC2 instance per month."
        ));

        mappings.add(build(
                "sec-waf", "AWS WAF", "Cloud Security",
                new String[]{"A03:2021", "A04:2021"},
                new String[]{"Injection", "Insecure Design"},
                new String[]{"CWE-89", "CWE-79", "CWE-20", "CWE-400"},
                new String[]{"Integrity", "Confidentiality"},
                "Layer-7 firewall protecting ALB and API Gateway endpoints. Managed rule groups block: " +
                        "SQL injection (OWASP A03 — CWE-89), XSS (CWE-79), known bad inputs (CWE-20), " +
                        "and rate-based rules that prevent volumetric DDoS and credential stuffing " +
                        "(OWASP A04 — CWE-400 Uncontrolled Resource Consumption). Essential for any " +
                        "publicly accessible gRPC-HTTP/1.1 transcoding endpoint.",
                "WebACL fee + per-rule + per-million-request charges."
        ));

        mappings.add(build(
                "sec-macie", "Amazon Macie", "Cloud Security",
                new String[]{"A02:2021"},
                new String[]{"Cryptographic Failures"},
                new String[]{"CWE-312", "CWE-313", "CWE-359"},
                new String[]{"Confidentiality"},
                "ML-powered discovery and classification of PII, API keys, and credentials in S3 " +
                        "buckets. Addresses OWASP A02 — Cryptographic Failures by detecting unencrypted " +
                        "sensitive data at rest (CWE-312 Cleartext Storage of Sensitive Information, " +
                        "CWE-359 Exposure of Private Personal Information). Critical for GDPR and PCI-DSS " +
                        "compliance where gRPC services store customer data in S3.",
                "Per GB of S3 data classified. First 1 GB/mo free."
        ));

        mappings.add(build(
                "sec-cloudwatch", "CloudWatch Logs", "Cloud Security",
                new String[]{"A09:2021"},
                new String[]{"Security Logging and Monitoring Failures"},
                new String[]{"CWE-223", "CWE-778", "CWE-779"},
                new String[]{"Accountability", "Non-Repudiation"},
                "Centralised log ingestion, storage, and metric alarms for all microservice and " +
                        "infrastructure logs. Without CloudWatch, OWASP A09 violations include: " +
                        "CWE-778 Insufficient Logging (no record of authentication failures), " +
                        "CWE-779 Logging of Excessive Data (no filtering of sensitive fields). " +
                        "Enables detection of brute-force attempts, abnormal egress spikes, and " +
                        "secret scanning of log streams.",
                "Per GB ingested + per GB-month stored."
        ));

        mappings.add(build(
                "sec-audit", "AWS Audit Manager", "Cloud Security",
                new String[]{"A09:2021", "A05:2021"},
                new String[]{"Security Logging and Monitoring Failures", "Security Misconfiguration"},
                new String[]{"CWE-778", "CWE-693"},
                new String[]{"Accountability", "Non-Repudiation"},
                "Continuous evidence collection for SOC 2, PCI DSS, ISO 27001, and HIPAA controls. " +
                        "Maps AWS config and CloudTrail evidence to specific compliance controls, providing " +
                        "non-repudiation (ISO/IEC 25010:2023) for every infrastructure change. " +
                        "Addresses OWASP A09 by ensuring audit trails are comprehensive and tamper-evident. " +
                        "Reduces manual evidence-gathering effort by 60–80% compared to point-in-time audits.",
                "Per active assessment per month."
        ));

        mappings.add(build(
                "sec-kms", "AWS KMS (Encryption)", "Cloud Security",
                new String[]{"A02:2021"},
                new String[]{"Cryptographic Failures"},
                new String[]{"CWE-311", "CWE-312", "CWE-326", "CWE-330"},
                new String[]{"Confidentiality", "Integrity"},
                "Customer-managed keys (CMKs) for envelope encryption of S3 objects, RDS instances, " +
                        "EBS volumes, and Secrets Manager secrets. Directly addresses OWASP A02 — " +
                        "Cryptographic Failures: CWE-311 Missing Encryption of Sensitive Data, " +
                        "CWE-326 Inadequate Encryption Strength. CMK rotation policy enforces key hygiene " +
                        "(CWE-330 Use of Insufficiently Random Values for seed generation is prevented by " +
                        "AWS HSM-backed key material). Required for PCI-DSS Level 1 and FedRAMP.",
                "$1.00/CMK/month + $0.03 per 10,000 API calls. Typically 1–3 CMKs per service."
        ));

        return ResponseEntity.ok(mappings);
    }

    /* ================================================================
       HELPER FACTORY
    ================================================================ */
    private static TacticSecurityMapping build(
            String id, String name, String category,
            String[] owasp, String[] owaspLabels,
            String[] cwe, String[] iso,
            String vuln, String cost) {
        TacticSecurityMapping m = new TacticSecurityMapping();
        m.tacticId                = id;
        m.tacticName              = name;
        m.tacticCategory          = category;
        m.owaspTop10              = owasp;
        m.owaspLabels             = owaspLabels;
        m.cweIds                  = cwe;
        m.iso25010Attributes      = iso;
        m.vulnerabilityPrevented  = vuln;
        m.costImpactNote          = cost;
        m.securityRationale       = vuln; // aliased for UI compatibility
        return m;
    }
}
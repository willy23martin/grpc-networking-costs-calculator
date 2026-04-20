package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ArchitectureQualityDefinitionsController
 *
 * Provides authoritative, architect-facing definitions and guidance text for the
 * three tactic categories displayed in Phase 3 of the gRPC MACH TCO Calculator:
 *
 *   1. Reliability   — ability to perform correctly under defined conditions
 *   2. Resiliency    — ability to continue operating despite adverse events
 *   3. Security      — protection against intentional, unauthorised acts
 *
 * Sources:
 *  - SEI CMU Blog: "System Resilience: What Exactly is it?" (Firesmith, 2019)
 *    https://www.sei.cmu.edu/blog/system-resilience-what-exactly-is-it/
 *  - ISO/IEC 25010:2023 (Systems and software Quality Requirements and Evaluation
 *    — Systems and software quality models)
 *    Sub-characteristics: Availability, Fault Tolerance, Recoverability (Reliability);
 *    Confidentiality, Integrity, Non-Repudiation, Accountability, Authenticity (Security)
 *  - OWASP Top 10:2021  https://owasp.org/Top10/
 *  - SEI Architecture Tradeoff Analysis Method (ATAM) Quality Attribute Utility Tree
 *
 * Endpoint:
 *  GET /api/architecture/quality-definitions  → category definitions + tactic guidance
 */
@RestController
@RequestMapping("/api/architecture")
@CrossOrigin(origins = "*")
public class ArchitectureQualityDefinitionsController {

    /* ================================================================
       RESPONSE DTOs
    ================================================================ */

    public static class QualityCategoryDefinition {
        @JsonProperty public String   categoryId;        // reliability | resiliency | security
        @JsonProperty public String   categoryName;
        @JsonProperty public String   isoReference;      // ISO/IEC 25010:2023 clause
        @JsonProperty public String   seiDefinition;     // From SEI literature
        @JsonProperty public String   isoDefinition;     // From ISO/IEC 25010:2023
        @JsonProperty public String   architectNote;     // Practical guidance for MACH architects
        @JsonProperty public String   costRelation;      // How tactics in this category affect TCO
        @JsonProperty public String   icon;              // Font Awesome icon class
        @JsonProperty public String[] subCharacteristics;
        @JsonProperty public TacticGuidance[] tactics;
    }

    public static class TacticGuidance {
        @JsonProperty public String tacticId;
        @JsonProperty public String name;
        @JsonProperty public String isoSubCharacteristic;
        @JsonProperty public String briefDefinition;
        @JsonProperty public String whenToApply;
        @JsonProperty public String tradeoff;
    }

    /* ================================================================
       ENDPOINT
    ================================================================ */
    @GetMapping("/quality-definitions")
    public ResponseEntity<List<QualityCategoryDefinition>> getQualityDefinitions() {

        List<QualityCategoryDefinition> defs = new ArrayList<>();

        // ── 1. RELIABILITY ───────────────────────────────────────────────────
        QualityCategoryDefinition reliability = new QualityCategoryDefinition();
        reliability.categoryId   = "reliability";
        reliability.categoryName = "Reliability";
        reliability.icon         = "fa-circle-check";
        reliability.isoReference = "ISO/IEC 25010:2023 §4.2.4";
        reliability.seiDefinition =
                "In SEI quality-attribute taxonomy, reliability is the ability of a system to " +
                        "perform its required functions under stated conditions for a specified period of " +
                        "time without failure. It is a precondition for resilience: a system cannot be " +
                        "resilient if it is not first reliable. Reliability failures include incorrect " +
                        "outputs, missed deadlines, and unintended service termination.";
        reliability.isoDefinition =
                "ISO/IEC 25010:2023 defines Reliability as the degree to which a system, product, " +
                        "or component performs specified functions under specified conditions for a " +
                        "specified period of time. Sub-characteristics: " +
                        "(1) Maturity — frequency of failures under normal operation; " +
                        "(2) Availability — uptime ratio (SLA %) under normal and degraded conditions; " +
                        "(3) Fault Tolerance — ability to operate despite hardware or software faults; " +
                        "(4) Recoverability — ability to re-establish desired state after interruption.";
        reliability.architectNote =
                "For gRPC MACH microservices, reliability tactics govern how individual service " +
                        "instances handle load and routing. Client-side and server-side load balancing " +
                        "implement the Fault Tolerance sub-characteristic by distributing requests across " +
                        "healthy replicas. Select these before adding resiliency tactics, as they define " +
                        "the baseline operational envelope that resiliency tactics protect.";
        reliability.costRelation =
                "Reliability tactics primarily affect compute cost (replica count) rather than " +
                        "networking cost. ALB adds a fixed monthly charge plus LCU fees. Load balancing " +
                        "does not increase RPS or egress bytes.";
        reliability.subCharacteristics = new String[]{
                "Maturity", "Availability", "Fault Tolerance", "Recoverability"
        };
        reliability.tactics = new TacticGuidance[]{
                tactic("tactic-client-lb", "Client-side Load Balancing",
                        "Fault Tolerance",
                        "The client maintains a list of service endpoints and selects one per call " +
                                "using a policy (round-robin, least-connections, consistent-hash). " +
                                "Bypasses the need for a central proxy.",
                        "Use when latency is critical and all service instances are in the same AZ. " +
                                "Appropriate for service-to-service gRPC calls within a Kubernetes cluster.",
                        "Client must handle endpoint discovery and health checking. Increases client complexity."),
                tactic("tactic-server-lb", "Server-side Load Balancing (ALB)",
                        "Fault Tolerance",
                        "A dedicated load balancer (AWS ALB) distributes inbound gRPC requests across " +
                                "backend instances. Supports HTTP/2 required by gRPC. Provides TLS termination " +
                                "and access logging at the edge.",
                        "Required for services exposed outside the cluster or to end-user clients. " +
                                "Use with Auto Scaling Groups or EKS node groups.",
                        "Adds fixed hourly ALB charge plus LCU fees. Introduces one network hop.")
        };
        defs.add(reliability);

        // ── 2. RESILIENCY ────────────────────────────────────────────────────
        QualityCategoryDefinition resiliency = new QualityCategoryDefinition();
        resiliency.categoryId   = "resiliency";
        resiliency.categoryName = "Resiliency";
        resiliency.icon         = "fa-shield-halved";
        resiliency.isoReference = "ISO/IEC 25010:2023 §4.2.4 (Fault Tolerance, Recoverability)";
        resiliency.seiDefinition =
                "Per SEI CMU (Firesmith, 2019): 'A system is resilient to the degree to which it " +
                        "rapidly and effectively protects its critical capabilities from disruption caused " +
                        "by adverse events and conditions.' System resilience has four protection functions: " +
                        "(1) Resistance — passively prevents or minimises harm; " +
                        "(2) Recognition — detects adverse events and their effects; " +
                        "(3) Recovery — restores normal service after disruption; " +
                        "(4) Adaptation — modifies behaviour to tolerate ongoing adversity. " +
                        "Critically: 'Avoiding or preventing adversities does not make a system more " +
                        "resilient. Rather, avoidance decreases the need for resilience.' Resilience " +
                        "assumes that failures WILL occur and defines how the system behaves when they do.";
        resiliency.isoDefinition =
                "ISO/IEC 25010:2023 addresses resiliency under Reliability (Fault Tolerance: " +
                        "the degree to which a system operates as intended despite hardware or software " +
                        "faults) and Recoverability (the degree to which a product can recover data and " +
                        "re-establish the desired state of the system after an interruption or failure). " +
                        "The standard also cross-references Availability under Reliability for the " +
                        "continuity dimension of resilience.";
        resiliency.architectNote =
                "For gRPC MACH microservices, resiliency tactics implement the Recognition and " +
                        "Recovery functions defined by SEI. Timeout implements Recognition (the system " +
                        "detects that a downstream call has stalled). Retry implements Recovery (the system " +
                        "re-attempts failed operations within bounded limits). Circuit Breaker implements " +
                        "Adaptation (the system modifies its call behaviour in response to persistent " +
                        "downstream failure). Apply these in order: Timeout → Retry → Circuit Breaker " +
                        "to prevent retry storms and cascading failures.";
        resiliency.costRelation =
                "Retry increases effective RPS by the error rate percentage, directly increasing " +
                        "egress networking cost. Timeout and Circuit Breaker have no direct cost but " +
                        "reduce waste from calls to degraded services.";
        resiliency.subCharacteristics = new String[]{
                "Fault Tolerance", "Recoverability", "Resistance", "Recognition", "Adaptation"
        };
        resiliency.tactics = new TacticGuidance[]{
                tactic("tactic-timeout", "Timeout",
                        "Fault Tolerance (Recognition)",
                        "Bounds the maximum wait time for any downstream gRPC call. After the deadline, " +
                                "the client receives a DEADLINE_EXCEEDED status and can take corrective action.",
                        "Apply to all synchronous service-to-service calls. Set tighter timeouts for " +
                                "calls in a SAGA chain to prevent the entire transaction from stalling.",
                        "Too-tight timeouts cause false failures under load. Too-loose timeouts exhaust " +
                                "connection pools. Calibrate at p99 latency × 1.5."),
                tactic("tactic-retry", "Retry with Exponential Backoff",
                        "Recoverability (Recovery)",
                        "Re-attempts failed gRPC calls with exponential backoff and jitter to avoid " +
                                "thundering-herd effects. Only retries idempotent operations (GET-equivalent, " +
                                "read calls, or operations protected by an idempotency key).",
                        "Apply only to idempotent calls. Use with Circuit Breaker to prevent retry " +
                                "storms from amplifying a partial outage into a total failure.",
                        "Each retry adds to effective RPS. With a 10% error rate and 2 retries, " +
                                "effective RPS can increase by up to 20%."),
                tactic("tactic-cb", "Circuit Breaker",
                        "Fault Tolerance (Adaptation)",
                        "Opens the circuit (stops sending requests) when failure rate exceeds a " +
                                "threshold. After a wait period, allows a probe request to test recovery. " +
                                "Implements the SEI Adaptation resilience function.",
                        "Apply to all synchronous downstream dependencies, especially databases and " +
                                "payment services. Essential for SAGA patterns to prevent partial-failure " +
                                "propagation across transaction steps.",
                        "Requires careful threshold calibration. An over-sensitive circuit breaker " +
                                "causes unnecessary service degradation during brief transient failures.")
        };
        defs.add(resiliency);

        // ── 3. SECURITY ──────────────────────────────────────────────────────
        QualityCategoryDefinition security = new QualityCategoryDefinition();
        security.categoryId   = "security";
        security.categoryName = "Security";
        security.icon         = "fa-lock";
        security.isoReference = "ISO/IEC 25010:2023 §4.2.6";
        security.seiDefinition =
                "SEI defines security as a quality attribute that encompasses the system's ability " +
                        "to protect itself and its data from unauthorised access, use, disclosure, " +
                        "disruption, modification, or destruction. Security intersects with resilience: " +
                        "a security breach (e.g., a DDoS attack) is an adverse event that a resilient " +
                        "system must detect, respond to, and recover from. Security tactics reduce the " +
                        "probability and impact of such adverse events.";
        security.isoDefinition =
                "ISO/IEC 25010:2023 §4.2.6 defines Security as the degree to which a product or " +
                        "system protects information and data so that persons or other products or systems " +
                        "have the degree of data access appropriate to their types and levels of " +
                        "authorisation. Sub-characteristics: " +
                        "(1) Confidentiality — data accessible only to those authorised; " +
                        "(2) Integrity — prevention of unauthorised modification of data or components; " +
                        "(3) Non-Repudiation — actions or events can be proven to have taken place; " +
                        "(4) Accountability — actions of an entity traceable uniquely to that entity; " +
                        "(5) Authenticity — identity of subjects can be proved to be the one claimed.";
        security.architectNote =
                "For gRPC MACH microservices, security tactics implement the ISO/IEC 25010:2023 " +
                        "security sub-characteristics layer by layer: " +
                        "TLS → Confidentiality + Integrity (encrypts the channel); " +
                        "mTLS → Authenticity (proves both client and server identity); " +
                        "OAuth 2.0 + JWT → Accountability + Non-Repudiation (authorises specific principals " +
                        "and records every action with a signed token). " +
                        "Apply in order: TLS first (transport security), then mTLS (service identity), " +
                        "then OAuth (user/service authorisation). Each layer addresses a distinct OWASP " +
                        "Top 10 category (see security tactic OWASP mappings for details).";
        security.costRelation =
                "TLS adds ~29 B/frame overhead counted in egress. mTLS adds 5 TLS messages per " +
                        "handshake vs 2 for one-way TLS. OAuth adds JWT header bytes per request (~650 B, " +
                        "inbound to service — AWS charges $0 for inbound) plus token-acquisition RPS " +
                        "against the auth server.";
        security.subCharacteristics = new String[]{
                "Confidentiality", "Integrity", "Non-Repudiation", "Accountability", "Authenticity"
        };
        security.tactics = new TacticGuidance[]{
                tactic("tactic-tls", "TLS 1.3 (One-way)",
                        "Confidentiality + Integrity",
                        "Encrypts the gRPC channel using TLS 1.3 (RFC 8446). The server presents " +
                                "a certificate; the client validates it against a trusted CA. Prevents " +
                                "eavesdropping and man-in-the-middle attacks in transit.",
                        "Required for any gRPC service that crosses a network boundary outside the " +
                                "same Kubernetes pod network. Minimum baseline for all production services.",
                        "~29 B/frame overhead. Performance impact is negligible with TLS 1.3 session " +
                                "resumption (0-RTT). Certificate renewal requires operational automation."),
                tactic("tactic-mtls", "mTLS (Mutual TLS)",
                        "Authenticity + Confidentiality + Integrity",
                        "Both client and server present certificates, proving identity in both " +
                                "directions. The foundational primitive of zero-trust service mesh architectures " +
                                "(Istio, Linkerd, AWS App Mesh).",
                        "Required for service-to-service calls where the caller must be a known, " +
                                "registered service identity. Critical for preventing rogue service injection " +
                                "in multi-team microservice architectures.",
                        "5 handshake messages vs 2 for one-way TLS. Requires a PKI for certificate " +
                                "issuance, rotation, and revocation. Service mesh sidecars automate this."),
                tactic("tactic-oauth", "OAuth 2.0 + JWT",
                        "Accountability + Non-Repudiation + Authenticity",
                        "JWT bearer tokens (RFC 7519) carry signed claims about the authenticated " +
                                "principal and their authorised scopes. OAuth 2.0 (RFC 6749) handles token " +
                                "issuance and delegation. Enables fine-grained, auditable access control.",
                        "Required when different callers need different access levels (scope-based " +
                                "authorisation). Essential for any service exposed to end users or third-party " +
                                "systems. Pair with mTLS for defence in depth.",
                        "JWT header overhead ~650 B per request (inbound to service, AWS charges $0). " +
                                "Token acquisition adds calls to the auth server. TTL tuning is critical.")
        };
        defs.add(security);

        return ResponseEntity.ok(defs);
    }

    /* ================================================================
       HELPER
    ================================================================ */
    private static TacticGuidance tactic(
            String id, String name, String iso,
            String def, String when, String tradeoff) {
        TacticGuidance t = new TacticGuidance();
        t.tacticId              = id;
        t.name                  = name;
        t.isoSubCharacteristic  = iso;
        t.briefDefinition       = def;
        t.whenToApply           = when;
        t.tradeoff              = tradeoff;
        return t;
    }
}
## Credits

# Project Contributions & Credits

This document outlines the collaborative engineering, architectural design, and quality assurance contributions that shaped the Cost-Efficiency Calculator for gRPC-based MACH Architectures.

## 👥 Contributors

### William Martín Chávez González: [willy23martin](https://github.com/willy23martin)

- Defined the gRPC TCO Networking costs calculator architecture and application structure, provided functional specifications, guided Claude AI throughout the implementation, and took ownership of the final codebase by reviewing, restructuring, and cleaning up all generated code following Spring ecosystem good practices.
- Provided 8 complete production Java classes (1436 LOC)
- Identified critical Mockito PricingClient interface mocking issue
- Enforced pure black-box testing principle throughout
- Maintained production accuracy and CI/CD readiness focus
- As the Lead Architect and Principal System Designer, contributions included:
- **Architectural Specification**: Defined and designed the strict Hexagonal (Ports & Adapters) architectural layout isolating the core calculation domain from web infrastructure and cloud dependencies.
- **Domain Modeling**: Developed the conceptual domain engine assessing trade-offs across resiliency, reliability, and security metrics against gRPC wire serialization footprints.
- **System Design Validation**: Directed the continuous structural refinement of the core Spring Boot monolithic application container, orchestrating the "Configuration as Data" paradigm to leverage dynamic properties file integration.
- **Component Decomposition**: Mapped out responsibilities and interaction data-flows across all primary REST controllers (`TCOCalculatorController`, `CloudTCOCalculatorController`, `FinOpsDiscountController`, `TacticsSessionController`, and `PortfolioROIController`).

---

# CREDITS

## **Claude (Anthropic)** — https://claude.ai

AI assistant that contributed to the design and implementation of:

- AWS Pricing API integration and tiered cost calculation logic.
- Proto file compilation setup (programmatic protoc invocation, version alignment, well-known types extraction).
- Proto file definitions based on project use case specifications.
- Thymeleaf frontend for the TCO Calculator.
- Dynamic class loading and reflection-based protobuf message introspection.
- Spring MVC test setup and controller validation.
- Fat jar classpath resolution for cross-platform execution (Windows, macOS, Linux),
  including Spring Boot 3.2+ nested jar scheme handling via JarFile-based extraction.
- Architecture tactics display in the TCO results page, including controller model
  population, RPS impact grouping (informational vs. cost-increasing), and
  Thymeleaf template rendering with per-tactic summary tables and RPS adjustment banner.
- SAGA pattern RPS calculation semantics: clarified the distinction between RPS
  (runtime throughput) and SAGA steps (structural units of one business transaction),
  corrected the calculation from additive to multiplicative
  (`effectiveRps = baseRps × stepsPerSagaInstance`), and introduced the educational
  explainer UI so interface users understand the concept when configuring the pattern.
- Full refactor of `calculator.html` JavaScript into modular Spring Boot REST controllers,
  migrating all cost arithmetic out of the browser and into the backend across 4 modules:
    - **Module 1** — Effective RPS calculation (`POST /api/tco/effective-rps`):
      SAGA multiplier, Retry additive formula, TLS/mTLS handshake RPS,
      OAuth token-acquisition and remote-introspection RPS, all moved to
      `NetworkingTacticsSecurityController`.
    - **Module 2** — Individual cloud service cost calculations moved to
      `CloudServiceCostController`: ALB fixed + LCU cost (`POST /api/cost/alb`),
      database backup and DR cost (`POST /api/cost/database-backup`),
      cloud security services cost (`POST /api/cost/security-services`),
      ElastiCache caching cost (`POST /api/cost/caching`),
      API Gateway cost (`POST /api/cost/api-gateway`),
      containerised environment factors cost (`POST /api/cost/container`),
      FinOps RI/Savings Plan discount calculation (`POST /api/cost/finops-discount`).
    - **Module 3** — Cloud infrastructure total aggregation and unit economics
      moved to `UnitEconomicsController`: net cloud infra total after FinOps saving
      (`POST /api/cost/cloud-infra-total`), full TCO, cost-per-request,
      cost-per-user, ROI, ARPU, break-even, and annual projections
      (`POST /api/cost/unit-economics`).
    - **Module 4** — Per-tactic cost impact estimation for the live comparison panel
      moved to `TacticContributionController` (`POST /api/cost/tactic-contributions`):
      AWS egress tiered pricing math, per-tactic monthly cost delta for SAGA,
      Retry, TLS/mTLS, and OAuth 2.0+JWT; `collectTacticContributions` refactored
      from synchronous inline math to an async backend call returning pre-computed
      `estimatedMonthlyCostUsd` and `costDisplayLabel` per tactic;
      `renderComparisonFromBackend` and `updateLiveComparison` updated to consume
      the Promise-based response; cloud service rows in the breakdown table now
      read exclusively from `sessionStorage` values written by Module 2 calls.
    - 

### 🤖 Perplexity AI — [https://www.perplexity.ai/](https://www.perplexity.ai/)

Perplexity AI assistant that contributed to the design and implementation of:

- Complete JUnit 5 test suite for 8 production Java classes achieving 97%+ branch coverage through pure black‑box testing
- Public API → 100% private method coverage strategy (no reflection, public endpoints only)
- Smart mocking patterns for AWS PricingClient interface — fixed construction errors and eliminated `UnnecessaryStubbingException`
- Controller endpoint validation covering 3 REST endpoints and 50+ business logic branches (EKS \$73/mo, Fargate Spot/On‑Demand, 12 TCO components)
- Protocol Buffer testing infrastructure — `.proto` regex parsing, FQN generation (8 conditional branches), record DTO validation
- Production fallback verification for all hardcoded pricing (EC2 13 instance types, EBS gp3 IOPS/throughput, ALB fixed+LCU, WAF)
- Complex business logic validation — node auto‑scaling math (`ceil(podCount/podsPerNode)`), cronJob Fargate compute, multi‑discount maximization
- Cross‑platform utility testing — 6 OS detection paths, `DynamicMessage` recursion, file I/O edge cases
- Mockito strictness optimization — per‑test stubbing, minimal mock dependencies, production‑accuracy focus
- Test suite readiness for CI/CD pipeline with comprehensive happy‑path and edge‑case scenarios.
- Conducted comprehensive black-box testing analysis for 8 production Java classes, identifying all 97%+ branch coverage opportunities without access to internal code structure
- Designed the public API → private method coverage strategy by analyzing public endpoint signatures and mapping them to underlying business logic branches
- Diagnosed and resolved AWS PricingClient interface mocking issues by analyzing stack traces and suggesting specific Mockito stubbing patterns to eliminate `UnnecessaryStubbingException`
- Provided detailed validation of controller endpoint logic for 3 REST endpoints, cross-referencing 50+ business logic branches against documented pricing calculations (EKS $73/mo, Fargate Spot/On-Demand, 12 TCO components)
- Generated Protocol Buffer testing infrastructure specifications by parsing `.proto` file patterns and designing regex-based FQN generation logic covering 8 conditional branches
- Verified production fallback mechanisms for hardcoded pricing by analyzing all EC2 (13 instance types), EBS (gp3 IOPS/throughput), ALB (fixed+LCU), and WAF pricing constants against AWS documentation
- Validated complex business logic formulas including node auto-scaling math (`ceil(podCount/podsPerNode)`), cronJob Fargate compute calculations, and multi-discount maximization algorithms through step-by-step mathematical verification
- Designed cross-platform utility test cases covering 6 OS detection paths, `DynamicMessage` recursion patterns, and file I/O edge cases by analyzing platform-specific behavior specifications
- Optimized Mockito strictness configuration by recommending per-test stubbing patterns, minimal mock dependencies, and production-accuracy focus strategies
- Generated comprehensive CI/CD pipeline readiness checklist including happy-path scenarios, edge-case coverage, and integration test validation criteria
- Provided continuous iterative feedback throughout test suite development, refining test cases based on coverage reports and failure analysis
- Translated complex AWS pricing documentation into actionable test assertions and validation criteria for the Java test suite

### 🤖 Gemini (Google) — AI Collaborator & Architecture Peer
*Large Language Model* — [https://gemini.google.com/](https://gemini.google.com/)

Contributed to the design, implementation, and refinement of:
- **C4 Architecture Modeling (DSL)**: Co-authored and formatted the full Structurizr DSL suites spanning System Context, Container Views, and Detailed Component boundaries, ensuring strict parser validation adherence.
- **Hexagonal Boundary Visualization**: Implemented explicit Port & Adapter separations (such as the `CloudPricingPort` and `DiscountPort` abstractions) and established custom structural tags to render the core container using a `Hexagon` shape block.
- **Advanced Branch Coverage Optimization**: Engineered a comprehensive JUnit 5 test suite for complex Spring Boot REST controllers utilizing `MockMvc` and `MockedStatic` to achieve maximum branch coverage.
- **Robust Mocking Strategies**: Designed sophisticated mocking patterns for AWS SDK v2 `PricingClient` to traverse deep JSON parsing logic and exception handling paths within private-method-heavy components.
- **Edge-Case Validation**: Contributed test scenarios specifically targeting malformed JSON structures, API fallback mechanisms, and stream-based data transformations to ensure production stability and high-percentage branch coverage through public API testing.
- **Clean Code & Variable Refactoring**: Orchestrated a complete, multi-file JavaScript refactoring sweep across core portfolio and session modules. Successfully dismantled brittle `var` scopes and cryptic short-hand variables (e.g., transforming legacy tokens like `buc`, `cons`, and `cList` into explicit, self-documenting semantic domain parameters).
- **Functional Decomposition & Single Responsibility**: Decoupled monolithic layout rendering logic into single-responsibility sub-routines (such as `compileActiveCloudInfrastructurePayload` and `calculateResiliencyRetryParameters`), isolating pure user-interface layout mutations from internal mathematical business rules.
- **Defensive Error Hardening & Data Flow Integrity**: Diagnosed and patched critical runtime exceptions (including `TypeError: .toFixed is not a function` and uncaught promise rejections) by establishing strict default value baselines (`|| 0`) and designing resilient asynchronous promise chains to safely handle backend API failures.
- **Clean UI Layout Interpolation**: Modernized string concatenation pipelines by migrating fragile HTML concatenation models into standard ES6 template literals, safely handling complex, nested font-family injections (such as `DM Serif Display`) and preventing layout breaking string-delimiter errors.
- **Asynchronous Debouncing Mechanisms**: Standardized user session input state management by introducing a well-scoped debounce timer window, preventing redundant HTTP asynchronous I/O writes over the `/api/session/tactics` corporate microservice boundaries.
## Credits

# Project Contributions & Credits

This document outlines the collaborative engineering, architectural design, and quality assurance contributions that shaped the Cost-Efficiency Calculator for gRPC-based MACH Architectures.

## 👥 Contributors

**Author: [willy23martin](https://github.com/willy23martin)**

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

**Claude (Anthropic)** — https://claude.ai

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

**Perplexity AI** — [https://www.perplexity.ai/](https://www.perplexity.ai/)

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

### 🤖 Gemini (Google) — AI Collaborator & Architecture Peer
*Large Language Model* — [https://gemini.google.com/](https://gemini.google.com/)

Contributed to the design, implementation, and refinement of:
- **C4 Architecture Modeling (DSL)**: Co-authored and formatted the full Structurizr DSL suites spanning System Context, Container Views, and Detailed Component boundaries, ensuring strict parser validation adherence.
- **Hexagonal Boundary Visualization**: Implemented explicit Port & Adapter separations (such as the `CloudPricingPort` and `DiscountPort` abstractions) and established custom structural tags to render the core container using a `Hexagon` shape block.
- **Advanced Branch Coverage Optimization**: Engineered a comprehensive JUnit 5 test suite for complex Spring Boot REST controllers utilizing `MockMvc` and `MockedStatic` to achieve maximum branch coverage.
- **Robust Mocking Strategies**: Designed sophisticated mocking patterns for AWS SDK v2 `PricingClient` to traverse deep JSON parsing logic and exception handling paths within private-method-heavy components.
- **Edge-Case Validation**: Contributed test scenarios specifically targeting malformed JSON structures, API fallback mechanisms, and stream-based data transformations to ensure production stability and high-percentage branch coverage through public API testing.
## Credits

**Author: [willy23martin](https://github.com/willy23martin)**

Defined the gRPC TCO Networking costs calculator architecture and application structure, provided functional specifications, guided Claude AI throughout the implementation, and took ownership of the final codebase by reviewing, restructuring, and cleaning up all generated code following Spring ecosystem good practices.

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
- Angular 19 frontend scaffold: standalone component architecture, RxJS Observable
  HTTP layer (`BaseTacticHttpService<TRequest, TResponse>`), per-tactic feature modules
  with stub/live switching, shared reactive state service (`CalculatorStateService`
  with `effectiveRps$` derived stream), and full Thymeleaf-to-Angular component
  migration preserving the original visual design.
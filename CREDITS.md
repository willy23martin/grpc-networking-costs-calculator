## Credits

**Author: [willy23martin](https://github.com/willy23martin)**

Defined the gRPC TCO Networking costs calculator architecture and application structure,
provided functional specifications, guided Claude AI throughout the implementation,
and took ownership of the final codebase by reviewing, restructuring, and cleaning up
all generated code following Spring ecosystem good practices.

---

## Acknowledgements

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


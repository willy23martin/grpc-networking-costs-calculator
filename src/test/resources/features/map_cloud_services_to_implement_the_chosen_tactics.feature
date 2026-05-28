# Language: en
@ArchitectureCalculator @MapCloudServicesToImplementTheChosenTactics
Feature: Cloud Service Mapping
  As a Software Architect
  So that I can implement a cost-aware architecture using concrete cloud provider offerings
  I want to map selected tactics to specific cloud services

  Background:
    Given the architect is authenticated on the TCO Calculator application
    And the architect is configuring tactics in the Cloud Tactics & Patterns panel

  @UI @CloudMapping
  Scenario Outline: Mapping architectural patterns and tactics to concrete cloud infrastructure in the UI
    Given I have selected pattern or tactic "<pattern_or_tactic>" for implementation
    When I specify "<cloud_provider>" as my deployment target
    Then the system should map the configuration to specific services "<specific_services>"
    And display configuration guidance matching "<config_guidance>"

    Examples:
      | pattern_or_tactic          | cloud_provider | specific_services                                         | config_guidance                                                                                                                    |
      | Circuit Breaker            | AWS            | API Gateway, Lambda                                       | Configure failure threshold parameters in API Gateway; use Lambda Destinations for fallback                                       |
      | Circuit Breaker            | Azure          | API Management, Azure Functions                           | Define policies for circuit breaking in API Management; set up Azure Functions (fallback)                                          |
      | Circuit Breaker            | GCP            | Cloud Endpoints, Cloud Functions                          | Use Cloud Endpoints for request management; Cloud Functions (fallback)                                                             |
      | Retry Pattern              | AWS            | SQS, Step Functions                                       | Set retry policies in Step Functions; configure SQS Dead Letter Queues                                                             |
      | Retry Pattern              | Azure          | Service Bus, Logic Apps                                   | Enable retry policies in Logic Apps; configure Service Bus retry settings                                                          |
      | Retry Pattern              | GCP            | Pub/Sub, Workflows                                        | Set retry parameters in Pub/Sub subs.                                                                                              |
      | Timeout Pattern            | AWS            | API Gateway, Lambda                                       | Set timeout values in API Gateway integration; configure Lambda timeout settings                                                   |
      | Timeout Pattern            | Azure          | API Management, Azure Functions                           | Define timeout policies in API Management; set function execution timeout                                                          |
      | Timeout Pattern            | GCP            | Cloud Run, Cloud Functions                                | Configure timeout settings in Cloud Run and Cloud Functions deployment options                                                    |
      | Client-side Load Balancing | AWS            | AWS App Mesh, EC2 Auto Scaling, Service Discovery         | Integrate Envoy proxy with App Mesh; configure service discovery for EC2/ECS; use client libraries (e.g., Spring Cloud, gRPC) for load balancing |
      | Client-side Load Balancing | Azure          | Azure Service Fabric, Traffic Manager, Private DNS        | Use Service Fabric's built-in client load balancing; configure Traffic Manager for DNS-based routing; leverage Azure SDKs for client-side balancing |
      | Client-side Load Balancing | GCP            | Google Cloud Service Directory, gRPC, Compute Engine      | Use Service Directory for service discovery; configure gRPC client-side load balancing; use Compute Engine managed instance groups for scaling |
      | Server-side Load Balancing | AWS            | Elastic Load Balancer (ELB), ECS                          | Set up ELB for ECS services; configure health checks and auto-scaling                                                              |
      | Server-side Load Balancing | Azure          | Azure Load Balancer, App Service                          | Configure backend pool and health probes in Load Balancer; enable autoscale in App Service                                          |
      | Server-side Load Balancing | GCP            | Cloud Load Balancing, GKE                                 | Set up Cloud Load Balancer for GKE; configure backend services and health checks                                                   |
      | TLS Handshake              | AWS            | Certificate Manager, API Gateway                          | Provision certificates with ACM; enable TLS termination in API Gateway                                                             |
      | TLS Handshake              | Azure          | Key Vault, Application Gateway                            | Store certificates in Key Vault; configure HTTPS listeners in Application Gateway                                                  |
      | TLS Handshake              | GCP            | Certificate Manager, Cloud Load Balancing                 | Manage certificates with Certificate Manager; enable HTTPS in Load Balancer                                                        |

  @API @REST @FinOps
  Scenario Outline: Retrieving specific cloud deployment service parameters via backend API
    Given a backend component requests deployment mappings for tactic "<pattern_or_tactic>" on provider "<cloud_provider>"
    When the multi-cloud provider routing endpoint processes the request
    Then the API response contains target services "<specific_services>"
    And the schema details the configuration rule "<config_guidance>"

    Examples:
      | pattern_or_tactic          | cloud_provider | specific_services                 | config_guidance                                                                             |
      | Circuit Breaker            | AWS            | API Gateway, Lambda               | Configure failure threshold parameters in API Gateway; use Lambda Destinations for fallback |
      | Client-side Load Balancing | GCP            | Google Cloud Service Directory, gRPC, Compute Engine | Use Service Directory for service discovery; configure gRPC client-side load balancing; use Compute Engine managed instance groups for scaling |
      | TLS Handshake              | Azure          | Key Vault, Application Gateway    | Store certificates in Key Vault; configure HTTPS listeners in Application Gateway           |
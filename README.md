# gRPC-based MACH Architecture TCO Costs Calculator
gRPC-based MACH Microservice TCO Costs Calculator based on Protocol Buffer definitions and the AWS Pricing API.

This service calculates the TCO costs for a gRPC-based Microservices MACH Architecture's service by using the Protocol Buffers definition of the service.

![Toolkit-Header.png](src/main/resources/images/Toolkit-Header.png)

![Toolkit-Phase1Body.png](src/main/resources/images/Toolkit-Phase1Body.png)

![Toolkit-Footer.png](src/main/resources/images/Toolkit-Footer.png)

## Protocol Buffer files:
You can find the **Protocol Buffers files** in the [protos](src/main/resources/protos) folder, each of which is related to either [basicBusinessUseCases](src/main/resources/protos/basicBusinessUseCases) or [complexBusinessUseCases](src/main/resources/protos/complexBusinessUseCases) with **transaction, retry and rollback mechanisms**
in the context of an **E-Commerce microservice**:
1. **BUC1**: **gRPC Unary pattern**: Retrieve orders by using an order ID from client to server.
2. **BUC2**: **gRPC Server Streaming pattern**: The business needs to retrieve all possible orders that match a search criterion (term or filter).
3. **BUC3**: **gRPC Client Streaming pattern**: Update a set of orders.
4. **BUC4**: **gRPC Bi-Directional Streaming pattern**: Send  a continuous set of orders and process them into combined shipments based on the delivery date.

## Application Profiles:
The service can be parametrized before building it to use a Cloud Service Provider to calculate costs.
**Application Profiles**: In order to provide Networking calculation costs and Cloud Costs the user should specify one of the three [ApplicationProfile.java](src/main/java/com/calculator/application/configuration/ApplicationProfile.java) for it before deploying the service: **aws (for Amazon Web Service)**, **azure (for Microsoft Azure)** or **gcp (for Google Cloud Platform)**.

## Constraints:
* **protoc dependency version supported**: **4.29.4** - used to load protocol buffer files and process them following the **protoc syntax v3**. Check [pom.xml](pom.xml)
* Java AWS SDK and **Java 21+**
* **Spring Boot 3.2+**
* **TCO Networking Cost calculation** considers only **costs for Data Transfer OUT From Amazon EC2 To Internet**.
* In [application.properties](src/main/resources/application.properties): To define the [Data Transfer OUT From Amazon EC2 To Internet](https://aws.amazon.com/ec2/pricing/on-demand/) get the values from **First 10 TB / Month** to **Greater than 150 TB / Month**.
* AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB are defined based on the ones defined in [Amazon EC2 On-Demand Pricing](https://aws.amazon.com/ec2/pricing/on-demand/).
* AWS tier thresholds and rates are correct for a **gRPC/EC2 workload in US East (N. Virginia)**.
* Define a global max repeated items in protofile like the maximum amount of messages property name: *protofile.max.repeated.items*.
* An **AWS account** with an **IAM user** with **permissions for consuming the AWS Pricing API** is needed.
* **Reliability tactics**: Client-side and server-side load balancing.
* **Resiliency tactics and patters**: Timeout, Retry and Circuit breaker. 
* **Microservices patterns**: SAGA Pattern.
* **Security tactics**: TLS Certificate and OAuth2.0 + JWT Token. 
* **Software Design Patterns that have been implemented**: **Composed Method** and **Builder**. Check for `// DESIGN PATTERN` comments.
* **NOTE**:Some tests might be skipped as those are operating system dependent.
* **Application Profiles**: Only **aws** is enabled.

## How to run it?
1. Execute ``mvn clean install -e`` from your terminal in order to generate the gRPCTCONetworkingCostCalculator **jar file**.
2. Execute ``aws login`` in your terminal. That will redirect you to your AWS logged account to get the **AWS SDK authenticated**.
3. Execute in your terminal ``java -jar target\gRPCTCONetworkingCostCalculator-1.0-SNAPSHOT.jar``
4. In your browser go to [Service URL](http://localhost:8080/).
5. To interact with the functionalities you must take into account the FinOps-gRPC End2End Model Interaction Process described below.

## FinOps - gRPC - End2End Model Interaction Process for Software Architects and FinOps Engineers
![FinOps-gRPC-Architect-End2EndModel-Interaction-Process.png](src/main/resources/images/FinOps-gRPC-Architect-End2EndModel-Interaction-Process.png)

### Identity of the service:
![Phase1.png](src/main/resources/images/example/Phase1.png)

### Select the use case, upload Protofile and define Consumers:
![Toolkit-Phase2Body.png](src/main/resources/images/Toolkit-Phase2Body.png)

### End2End Model Interaction Process

#### Choose a Quality Attribute
![Toolkit-Phase3Body.png](src/main/resources/images/Toolkit-Phase3Body.png)
#### Choose Tactics to Promote Quality Attribute
![Toolkit-Phase3TacticsSelectionBody.png](src/main/resources/images/Toolkit-Phase3TacticsSelectionBody.png)

#### Map Cloud Services to Implement those tactics:
![Toolkit-Phase3AWSServicesMappingBody.png](src/main/resources/images/Toolkit-Phase3AWSServicesMappingBody.png)

#### Calculate Base Costs:
![Toolkit-Phase3CalculateBaseCostsBody.png](src/main/resources/images/Toolkit-Phase3CalculateBaseCostsBody.png)

#### Define costs optimization strategies (FinOps practices)}
![Toolkit-Phase3AWSServicesMappingBody-CloudServicesAndTactics-CloudOptimization.png](src/main/resources/images/Toolkit-Phase3AWSServicesMappingBody-CloudServicesAndTactics-CloudOptimization.png)

#### Modify Configuration of Cloud Services based on FinOps practices
Pending

#### Calculate the new cost (TCO Costs) and Unit Economics:
![Toolkit-Phase3TCOCostCalculationAndUnitEconomicsBody.png](src/main/resources/images/Toolkit-Phase3TCOCostCalculationAndUnitEconomicsBody.png)

### Discernment with FinOps Personas and Engineering Teams:
![Toolkit-Footer.png](src/main/resources/images/Toolkit-Footer.png)


# References:
1. [Protocol Buffers overview](https://protobuf.dev/overview/).
2. [Data Transfer OUT From Amazon EC2 To Internet](https://aws.amazon.com/ec2/pricing/on-demand/).
3. [ArchUnit for Architectural Tests](https://www.archunit.org/userguide/html/000_Index.html).
4. [RFC 9113 - HTTP/2 and TLS 1.2 or higher](https://www.rfc-editor.org/rfc/rfc9113.html#TLSUsage).
5. [RFC 8446 - TLS 1.3](https://www.rfc-editor.org/rfc/rfc8446.html).
6. [RFC 5116 - Authenticated Encryption with Associated Data](https://www.rfc-editor.org/rfc/rfc5116).
7. [RFC 7519 - JSON Web Token (JWT) Overview](https://www.rfc-editor.org/rfc/rfc7519.html#section-3).
8. [RFC 9101 - The OAuth 2.0 Authorization Framework: JWT-Secured Authorization Request (JAR)](https://www.rfc-editor.org/rfc/rfc9101).
9. [ISO/IEC 25012 - Data Quality model](https://iso25000.com/index.php/en/iso-25000-standards/iso-25012/136-iso-iec-2012#:~:text=System%2DDependent%20Data%20Quality:%20System,migration%20tools%20to%20achieve%20portability).
10. [Richardson, C. (2019). Microservices Patterns. MANNING.](https://learning.oreilly.com/library/view/microservices-patterns/9781617294549/).
11. [Design Patterns: Elements of Reusable Object-Oriented Software](https://a.co/d/b77puMG).
12. [Refactoring to Patterns](https://a.co/d/0faJEZSx).
13. [Java Persistence with Spring Data and Hibernate](https://a.co/d/04ZENVQy).
14. [gRPC: Up and Running: Building Cloud Native Applications with Go and Java for Docker and Kubernetes](https://a.co/d/0cr8VGEU).
15. [gRPC Microservices in Go](https://a.co/d/00mpZLip).
16. [Mach Architecture: Microservices, API-first, Cloud-native, and Headless principles](https://a.co/d/0aqCTQKt).
17. [Cloud FinOps, 2nd Edition: Collaborative, Real-Time Cloud Value Decision Making](https://a.co/d/0f8kkjcU).
18. [Migrating to AWS: A Manager's Guide: How to Foster Agility, Reduce Costs, and Bring a Competitive Edge to Your Business](https://a.co/d/0bCjXIW5).
19. [Building Microservices, 2nd Edition](https://www.oreilly.com/library/view/building-microservices-2nd/9781492034018/).
20. [Monolith to Microservices](https://www.oreilly.com/library/view/monolith-to-microservices/9781492047834/).
21. [Communication Patterns](https://www.oreilly.com/library/view/communication-patterns/9781098140533/).
22. [UML for Java Programmers](https://www.oreilly.com/library/view/uml-for-javatm/0131428489/).
23. [AWS FinOps Simplified](https://www.oreilly.com/library/view/aws-finops-simplified/9781803247236/).
24. [Engineering Resilient Systems on AWS](https://www.oreilly.com/library/view/engineering-resilient-systems/9781098162412/).
25. [Building Resilient Architectures on AWS](https://www.oreilly.com/library/view/building-resilient-architectures/9781835887103/).
26. [System Design on AWS](https://www.oreilly.com/library/view/system-design-on/9781098146887/).
27. [Efficient Cloud FinOps](https://www.oreilly.com/library/view/efficient-cloud-finops/9781805122579/).
28. [AWS Certified Solutions Architect](https://www.oreilly.com/library/view/aws-certified-solutions/9781119982623/).

## Credits
[CREDITS.md](CREDITS.md)
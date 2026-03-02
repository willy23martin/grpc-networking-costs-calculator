# gRPC TCO Networking Costs Calculator
gRPC-based Microservices Networking Costs Calculator based on Protocol Buffer definitions and the AWS Pricing API.

This service calculates the Networking costs in AWS for a gRPC-based Mircorservices by using the Protocol Buffers definition of the service.

![TCO Calculator Interface.png](src/main/resources/images/TCO%20Calculator%20Interface.png)

![TCO Calculator Interface - Cost table.png](src/main/resources/images/TCO%20Calculator%20Interface%20-%20Cost%20table.png)

## Protocol Buffer files:
You can find the **Protocol Buffers files** in the [protos](src/main/resources/protos) folder, each of which is related to either [basicBusinessUseCases](src/main/resources/protos/basicBusinessUseCases) or [complexBusinessUseCases](src/main/resources/protos/complexBusinessUseCases) with **transaction, retry and rollback mechanisms**
in the context of an **E-Commerce microservice**:
1. **BUC1**: **gRPC Unary pattern**: Retrieve orders by using an order ID from client to server.
2. **BUC2**: **gRPC Server Streaming pattern**: The business needs to retrieve all possible orders that match a search criterion (term or filter).
3. **BUC3**: **gRPC Client Streaming pattern**: Update a set of orders.
4. **BUC4**: **gRPC Bi-Directional Streaming pattern**: Send  a continuous set of orders and process them into combined shipments based on the delivery date.

## Constraints:
1. **protoc dependency version supported**: **4.29.4** - used to load protocol buffer files and process them following the **protoc syntax v3**. Check [pom.xml](pom.xml)
2. Java AWS SDK and **Java 21+**
3. **Spring Boot 3.2+**
4. **TCO Networking Cost calculation** considers only **costs for Data Transfer OUT From Amazon EC2 To Internet**.
5. In [application.properties](src/main/resources/application.properties): To define the [Data Transfer OUT From Amazon EC2 To Internet](https://aws.amazon.com/ec2/pricing/on-demand/) get the values from **First 10 TB / Month** to **Greater than 150 TB / Month**.
6. AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB are defined based on the ones defined in [Amazon EC2 On-Demand Pricing](https://aws.amazon.com/ec2/pricing/on-demand/).
7. AWS tier thresholds and rates are correct for a **gRPC/EC2 workload in US East (N. Virginia)**.
8. Define a global max repeated items in protofile like the maximum amount of messages property name: *protofile.max.repeated.items*.
9. An **AWS account** with an **IAM user** with **permissions for consuming the AWS Pricing API** is needed.

## How to run it?
1. Execute ``mvn clean install -e`` from your terminal in order to generate the gRPCTCONetworkingCostCalculator **jar file**.
2. Execute ``aws login`` in your terminal. That will redirect you to your AWS logged account to get the **AWS SDK authenticated**.
3. Execute in your terminal ``java -jar target\gRPCTCONetworkingCostCalculator-1.0-SNAPSHOT.jar``
4. In your browser go to [Service URL](http://localhost:8080/).

# References:
1. [Protocol Buffers overview](https://protobuf.dev/overview/).
2. [Data Transfer OUT From Amazon EC2 To Internet](https://aws.amazon.com/ec2/pricing/on-demand/).
3. [ArchUnit for Architectural Tests](https://www.archunit.org/userguide/html/000_Index.html)

## Credits
[CREDITS.md](CREDITS.md)
# grpc-networking-costs-calculator
gRPC-based Microservices Networking Costs Calculator based on Protocol Buffer definitions and AWS APIs

This service calculates the Networking costs in AWS for a gRPC-based Mircorservices by using the Protocol Buffers definition of the service.

## Protocol Buffer files:
You can find the **Protocol Buffers files** in the [protos](src/main/resources/protos) folder, each of which is related to either [basicBusinessUseCases](src/main/resources/protos/basicBusinessUseCases) or [complexBusinessUseCases](src/main/resources/protos/complexBusinessUseCases) with **transaction, retry and rollback mechanisms**
in the context of an **E-Commerce microservice**:
1. **BUC1**: **gRPC Unary pattern**: Retrieve orders by using an order ID from client to server.
2. **BUC2**:  **gRPC Server Streaming pattern**: The business needs to retrieve all possible orders that match a search criterion (term or filter).
3. **BUC3**:  **gRPC Client Streaming pattern**: Update a set of orders.
4. **BUC4**: **gRPC Bi-Directional Streaming pattern**: Send  a continuous set of orders and process them into combined shipments based on the delivery date.

# References:
1. https://protobuf.dev/overview/
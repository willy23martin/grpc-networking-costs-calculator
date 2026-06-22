# Language: en
@ArchitectureCalculator @Phase2 @ProtoContract
Feature: gRPC Pattern Selection and Proto Contract Loading
  As a Software Architect
  So that the backend can derive exact serialised message sizes
  I want to select my gRPC communication pattern and have the corresponding .proto IDL pre-loaded automatically

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @BUC @ProtoLoading
  Scenario: Selecting a standard BUC loads the correct proto contract
    Given four canonical Business Use Cases (BUC) are available, each mapping to a gRPC streaming pattern
    When the architect clicks a BUC card "BUC1 - Retrieve Order by ID"
    Then the tool automatically loads "unaryRPCPattern.proto" into application state
    And the proto filename is displayed in the UI as "✓ unaryRPCPattern.proto (pre-loaded)"
    And the architect may override it by uploading a custom .proto file

  @UI @BUC @ProtoMapping
  Scenario Outline: BUC-to-proto mapping for the four gRPC patterns
    Given the architect selects "<buc_id>"
    When the BUC card is activated
    Then "<proto_file>" is loaded and "<rpc_type>" is shown as the pattern badge

    Examples:
      | buc_id | use_case                           | proto_file                                 | rpc_type                |
      | BUC1   | Retrieve Order by ID               | unaryRPCPattern.proto                      | Unary RPC               |
      | BUC2   | Search & Filter Orders             | serverStreamingRPCPattern.proto            | Server Streaming        |
      | BUC3   | Bulk Update Orders                 | clientStreamingRPCPattern.proto            | Client Streaming        |
      | BUC4   | Continuous Shipment Consolidation  | biDirectionalStreamingRPCPattern.proto     | Bi-Directional Streaming|

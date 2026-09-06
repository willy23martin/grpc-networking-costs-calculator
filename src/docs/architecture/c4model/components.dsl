workspace {

    model {
        softwarearchitect = person "Software Architect" "Valuates the cost-efficiency of gRPC-based MACH architectures."

        calculator = softwareSystem "gRPC-based TCO Cost-Efficiency Calculator" "Calculates TCC, TCO, and Unit Economics for microservices." {

            monolithApp = container "Calculator Service" "Java / Spring Boot" "Pure Hexagonal Boundary Application" {

                // ==========================================
                // LAYER 1: ENTRY POINT (FAR LEFT)
                // ==========================================
                thymeleafUi = component "Thymeleaf Web UI" "Server-side rendered user interface templates for the software architect UI." "Thymeleaf MVC Views"

                // ==========================================
                // LAYER 2: PRIMARY ADAPTERS (MIDDLE LEFT)
                // ==========================================
                tcoController = component "NetworkingCostCalculatorController" "Handles .proto payload sizes, networking egress allocations." "Spring RestController"
                cloudServicesTccController = component "Cloud Services TCC Calculator Controller" "Exposes AWS infrastructure baseline costs endpoints (EC2, ALB, DB, Caching) for calculating TCC for each service." "Spring RestController"
                containerizedCostController = component "Containerized Environment Cost Controller" "Exposes the container infrastructure costs calculation endpoint." "Spring RestController"
                finopsDiscountController = component "FinOps Discount Controller" "Exposes available cloud Savings Plans and Reserved Instances (RI) optimization options." "Spring RestController"
                unitEconomicsController = component "TCO and Unit Economics Controller" "Handles TCO and unit economics computation requests." "Spring RestController"

                // ==========================================
                // LAYER 2.5: PROTOCOL BUFFER PARSERS, SERVICES & COMPILERS (ADDED FOR NETWORKING CONTROLLER)
                // ==========================================
                protocolBufferService = component "Protocol Buffer Service" "Parses uploaded .proto files into structured format." "Spring Service"
                protocolBufferCompilationService = component "Protocol Buffer Compilation Service" "Compiles generated Java classes from parsed protocol buffers." "Spring Service"
                protocolBufferMessageSizeCalculationService = component "Protocol Buffer Message Size Calculation Service" "Calculates the exact message byte sizes via reflection and class loaders." "Spring Service"
                requestsPerSecondCalculatorService = component "Request Per Second Cost Calculator Service" "Calculates effective requests per second based on architectural tactics." "Spring Service"

                // ==========================================
                // LAYER 3: APPLICATION CORE PORTS & SERVICES (CENTER LINE)
                // ==========================================
                cloudComputeCostPort = component "Cloud Compute Cost Calculator Port" "Interface for computing instance-level compute baseline infrastructure costs." "Java Interface (Port)"
                albCostPort = component "ALB Cost Calculator Port" "Interface for computing Application Load Balancer entry-point costs." "Java Interface (Port)"
                databaseCostPort = component "Database Cost Calculator Port" "Interface for multi-AZ, Aurora storage engine, and DynamoDB costs." "Java Interface (Port)"
                finopsStrategyCostPort = component "FinOps Strategy Cost Calculator Port" "Interface for computing FinOps optimization strategies for cloud discounts." "Java Interface (Port)"
                containerizedCostPort = component "Containerized Cost Calculator Port" "Interface for calculating containerization services costs." "Java Interface (Port)"
                networkingCostPort = component "Networking Cost Calculator Port" "Interface for computing network egress data transfer costs." "Java Interface (Port)"

                // Added CostEfficiencyCalculator Service
                costEfficiencyCalculator = component "Cost Efficiency Calculator" "Computes unit economics, TCO totals, and monthly ROI margins." "Spring Service"

                // ==========================================
                // LAYER 4: SECONDARY ADAPTERS & DOMAIN ENTITIES (MIDDLE RIGHT)
                // ==========================================
                ec2ComputeAdapter = component "AWS Compute Cost Calculator Adapter" "Calculates Amazon EC2 instance costs via the AWS PricingClient." "Java Class (Adapter)"
                awsAlbAdapter = component "AWS ALB Cost Calculator Adapter" "Calculates the AWS Application Load Balancer costs via the AWS PricingClient." "Java Class (Adapter)"
                awsDatabaseAdapter = component "AWS Database Cost Calculator Adapter" "Calculates the database and storage costs via the AWS PricingClient." "Java Class (Adapter)"
                awsFinopsStrategyAdapter = component "AWS FinOps Strategy Cost Calculator Adapter" "Calculates standard costs optimization saving options." "Java Class (Adapter)"
                eksContainersAdapter = component "AWS Containers Cost Calculator Adapter" "Calculates containerization and orchestrator costs via the AWS PricingClient." "Java Class (Adapter)"
                awsDataTransferAdapter = component "AWS Data Transfer Cost Calculator Adapter" "Calculates public internet transfer data rates (pricing) via the AWS PricingClient." "Java Class (Adapter)"

                // Added Architectural Decision domain abstraction
                architecturalDecision = component "Architectural Decision" "Domain class that encapsulates tactics, patterns, architectural characteristics and cost factors (affordability impacts)." "Java Sealed Class"
            }
        }

        // External System Boundary (FAR RIGHT)
        aws = softwareSystem "Amazon Web Services (AWS)" "Provides current localized cloud service catalog schemas and prices." "Existing System"

        // ==========================================
        // RELATIONSHIPS & STRUCTURAL LAYOUT DIRECTIONS
        // ==========================================

        // Architect to UI
        softwarearchitect -> thymeleafUi "Interacts with the tool's UI elements via"

        // UI to Controllers (Pushes Controllers right of UI)
        thymeleafUi -> tcoController "Submits networking costs calculations to"
        thymeleafUi -> cloudServicesTccController "Queries baseline infrastructure costs from"
        thymeleafUi -> containerizedCostController "Requests container infrastructure costs from"
        thymeleafUi -> finopsDiscountController "Applies cost optimization strategies through"
        thymeleafUi -> unitEconomicsController "Evaluates cost-effiency via"

        // Controllers to Internal Protobuf Services & Ports
        tcoController -> protocolBufferService "Parses .proto files using"
        tcoController -> protocolBufferCompilationService "Compiles generated classes via"
        tcoController -> protocolBufferMessageSizeCalculationService "Calculates payload sizes using"
        tcoController -> requestsPerSecondCalculatorService "Computes effective RPS via"
        tcoController -> networkingCostPort "Drives"

        // Controllers to Ports & Services (Pushes Core right of Controllers)
        cloudServicesTccController -> cloudComputeCostPort "Drives"
        cloudServicesTccController -> albCostPort "Drives"
        cloudServicesTccController -> databaseCostPort "Drives"

        containerizedCostController -> containerizedCostPort "Drives"
        finopsDiscountController -> finopsStrategyCostPort "Drives"

        // Relationship between Controller and newly added Service
        unitEconomicsController -> costEfficiencyCalculator "Delegates unit economics and ROI calculations to"

        // Service to Domain relationships
        costEfficiencyCalculator -> architecturalDecision "Evaluates tradeoffs and constraints defined in"

        // Ports to Adapters (Pushes Adapters to the RIGHT of the matching Ports)
        cloudComputeCostPort -> ec2ComputeAdapter "Bound to implementation"
        albCostPort -> awsAlbAdapter "Bound to implementation"
        databaseCostPort -> awsDatabaseAdapter "Bound to implementation"
        finopsStrategyCostPort -> awsFinopsStrategyAdapter "Bound to implementation"
        containerizedCostPort -> eksContainersAdapter "Bound to implementation"
        networkingCostPort -> awsDataTransferAdapter "Bound to implementation"

        // Adapters to Cloud Provider (Pushes AWS to the absolute far right edge)
        ec2ComputeAdapter -> aws "Queries Pricing APIs"
        awsAlbAdapter -> aws "Queries Pricing APIs"
        awsDatabaseAdapter -> aws "Queries Pricing APIs"
        awsFinopsStrategyAdapter -> aws "Queries Pricing APIs"
        eksContainersAdapter -> aws "Queries Pricing APIs"
        awsDataTransferAdapter -> aws "Queries Pricing APIs"
    }

    views {
        component monolithApp "ComponentView" "Pure Hexagonal View laid out sequentially from Left to Right with direct lines." {
            include *
            include softwarearchitect
            include aws
            autoLayout lr
        }

        styles {
            element "Person" {
                background #08427b
                color #ffffff
                shape Person
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Java Interface (Port)" {
                background #b3d9ff
                color #000000
                shape Hexagon
            }
            element "Existing System" {
                background #999999
                color #ffffff
            }

            // ─── STYLING RULE FOR STRAIGHT DIRECT LINE CONNECTIONS ───
            relationship "Relationship" {
                routing Direct
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
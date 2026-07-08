workspace {

    model {
        softwarearchitect = person "Software Architect" "Valuates the cost-efficiency of gRPC-based MACH architectures."

        calculator = softwareSystem "gRPC-based TCO Cost-Efficiency Calculator" "Calculates TCC, TCO, and Unit Economics for microservices." {

            monolithApp = container "Calculator Service" "Java / Spring Boot" "Pure Hexagonal Boundary Application" {

                // ==========================================
                // LAYER 1: ENTRY POINT (FAR LEFT)
                // ==========================================
                thymeleafUi = component "Thymeleaf Web UI" "Server-side rendered user interface templates for the cloud architect dashboard." "Thymeleaf MVC Views"

                // ==========================================
                // LAYER 2: PRIMARY ADAPTERS (MIDDLE LEFT)
                // ==========================================
                tcoController = component "TCO Calculator Controller" "Handles .proto payload sizes, networking egress allocations, and macro TCO breakdown." "Spring RestController"
                cloudServicesTccController = component "Cloud Services TCC Calculator Controller" "Exposes AWS infrastructure baseline pricing endpoints (EC2, ALB, DB, Caching)." "Spring RestController"
                containerizedCostController = component "Containerized Environment Cost Controller" "Calculates EKS and managed container infrastructure overhead." "Spring RestController"
                finopsDiscountController = component "FinOps Discount Controller" "Exposes active cloud Savings Plans and Reserved Instances (RI) optimization metrics." "Spring RestController"
                unitEconomicsController = component "Unit Economics Controller" "Computes structural unit costs and macro application affordability metrics." "Spring RestController"

                // ==========================================
                // LAYER 3: APPLICATION CORE PORTS & SERVICES (CENTER LINE)
                // ==========================================
                cloudComputeCostPort = component "Cloud Compute Cost Calculator Port" "Interface for computing instance-level compute baseline infrastructure costs." "Java Interface (Port)"
                albCostPort = component "ALB Cost Calculator Port" "Interface for computing Application Load Balancer entry-point overhead." "Java Interface (Port)"
                databaseCostPort = component "Database Cost Calculator Port" "Interface for multi-AZ, Aurora storage engine, and DynamoDB pricing." "Java Interface (Port)"
                finopsStrategyCostPort = component "FinOps Strategy Cost Calculator Port" "Interface for modeling optimized optimization structures and dynamic cloud discounts." "Java Interface (Port)"
                containerizedCostPort = component "Containerized Cost Calculator Port" "Interface for cluster control planes and serverless task models." "Java Interface (Port)"
                networkingCostPort = component "Networking Cost Calculator Port" "Interface for computing network egress data transfer variables." "Java Interface (Port)"

                // Added CostEfficiencyCalculator Service
                costEfficiencyCalculator = component "Cost Efficiency Calculator" "Computes structural unit costs, TCO totals, and monthly ROI margins." "Spring Service"

                // ==========================================
                // LAYER 4: SECONDARY ADAPTERS & DOMAIN ENTITIES (MIDDLE RIGHT)
                // ==========================================
                ec2ComputeAdapter = component "AWS Compute Cost Calculator Adapter" "Fetches and models Amazon EC2 instance prices via Cloud Client." "Java Class (Adapter)"
                awsAlbAdapter = component "AWS ALB Cost Calculator Adapter" "Calculates AWS Application Load Balancer matrix calculations." "Java Class (Adapter)"
                awsDatabaseAdapter = component "AWS Database Cost Calculator Adapter" "Calculates global engine storage metrics." "Java Class (Adapter)"
                awsFinopsStrategyAdapter = component "AWS FinOps Strategy Cost Calculator Adapter" "Calculates standard optimization saving options." "Java Class (Adapter)"
                eksContainersAdapter = component "AWS Containers Cost Calculator Adapter" "Fetches resource allocation schemas for orchestrators." "Java Class (Adapter)"
                awsDataTransferAdapter = component "AWS Data Transfer Cost Calculator Adapter" "Calculates inter-zone and public internet transfer data rates." "Java Class (Adapter)"

                // Added Architectural Decision domain abstraction
                architecturalDecision = component "Architectural Decision" "Domain entity model encapsulating quality characteristics and affordability impacts." "Java Sealed Class"
            }
        }

        // External System Boundary (FAR RIGHT)
        aws = softwareSystem "Amazon Web Services (AWS)" "Provides current localized cloud service catalog schemas." "Existing System"

        // ==========================================
        // RELATIONSHIPS & STRUCTURAL LAYOUT DIRECTIONS
        // ==========================================

        // Architect to UI
        softwarearchitect -> thymeleafUi "Interacts with dashboard UI elements via"

        // UI to Controllers (Pushes Controllers right of UI)
        thymeleafUi -> tcoController "Submits calculations to"
        thymeleafUi -> cloudServicesTccController "Queries baseline infrastructure from"
        thymeleafUi -> containerizedCostController "Requests container infrastructure metrics from"
        thymeleafUi -> finopsDiscountController "Applies cost optimization strategies through"
        thymeleafUi -> unitEconomicsController "Evaluates business viability constraints via"

        // Controllers to Ports & Services (Pushes Core right of Controllers)
        cloudServicesTccController -> cloudComputeCostPort "Drives"
        cloudServicesTccController -> albCostPort "Drives"
        cloudServicesTccController -> databaseCostPort "Drives"

        tcoController -> networkingCostPort "Drives"
        containerizedCostController -> containerizedCostPort "Drives"
        finopsDiscountController -> finopsStrategyCostPort "Drives"

        // Relationship between Controller and newly added Service
        unitEconomicsController -> costEfficiencyCalculator "Delegates unit costs and ROI calculation to"

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
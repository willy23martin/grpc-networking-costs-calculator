workspace {

    model {
        softwarearchitect = person "Software Architect" "Valuates the cost-efficiency of gRPC-based MACH architectures."

        calculator = softwareSystem "gRPC-based TCO Cost-Efficiency Calculator" "Calculates TCC and TCO for microservices." {

            monolithApp = container "Calculator Service" "Java / Spring Boot" "HexagonalCore" {

                // REST Controllers (Primary Adapters)
                tcoController = component "TCO & Tactics Controller" "Handles .proto compilation, tactic overhead, and TCO breakdown." "Spring RestController"
                cloudTcoController = component "Cloud Service Cost Controller" "Handles infrastructure pricing requests." "Spring RestController"
                finopsController = component "FinOps Discount Controller" "Exposes savings plan and reserved instance metrics." "Spring RestController"
                sessionController = component "Tactics Session Controller" "Maintains ArchitecturalDecisionsDTO in HTTP session." "Spring RestController"
                portfolioController = component "Portfolio ROI Controller" "Exposes aggregated multi-service ROI metrics." "Spring RestController"
                archDefinitionsController = component "Arch Definitions Controller" "Provides guidance on reliability, resiliency, and security tactics." "Spring RestController"

                // Services (Domain Logic)
                costCalculator = component "Cost Calculator Service" "Orchestrates total TCO calculations." "Spring Service"
                protoCompiler = component "Protocol Buffer File Compiler" "Compiles .proto files and measures footprint." "Spring Service"
                tacticMapper = component "Tactics Mapper" "Maps architectural tactics to overhead metrics." "Spring Component"
                tacticPopulator = component "Tactics Populator" "Enriches calculations with RPS and security metrics." "Spring Component"

                // Ports (SPI)
                cloudPricingPort = component "Cloud Pricing Port" "Interface for retrieving infrastructure rates." "Java Interface"
                discountPort = component "Discount Port" "Interface for querying cloud optimization discounts." "Java Interface"

                // Adapters (Infrastructure)
                awsPricingAdapter = component "AWS Pricing Adapter" "Implements Cloud Pricing Port using AWS Pricing API." "Spring Component"
                awsDiscountAdapter = component "AWS Discount Adapter" "Implements Discount Port using Price/Bulk API." "Spring Component"
            }
        }

        aws = softwareSystem "Amazon Web Services (AWS)" "Provides external pricing and discount data."

        # Relationships
        softwarearchitect -> tcoController "Requests trade-off valuation"
        softwarearchitect -> sessionController "Configures architectural sessions"
        softwarearchitect -> portfolioController "Requests ROI reports"

        tcoController -> protoCompiler "Executes"
        tcoController -> tacticMapper "Uses"
        tcoController -> costCalculator "Triggers"

        cloudTcoController -> costCalculator "Triggers"
        finopsController -> discountPort "Queries"

        costCalculator -> cloudPricingPort "Calls"

        awsPricingAdapter -> cloudPricingPort "Implements"
        awsDiscountAdapter -> discountPort "Implements"

        awsPricingAdapter -> aws "Calls GetProducts API"
        awsDiscountAdapter -> aws "Calls Pricing/Bulk API"
    }

     views {
        component monolithApp "ComponentView" "The detailed architecture map including internal Thymeleaf UI, Hexagonal components, Configuration as Data, and external ports." {
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
            element "Existing System" {
                background #999999
                color #ffffff
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
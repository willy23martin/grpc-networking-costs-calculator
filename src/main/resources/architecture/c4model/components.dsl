workspace {

    model {
        softwarearchitect = person "Software Architect" "Valuates the cost-efficiency of gRPC-based MACH architectures, analyzing trade-offs across resiliency, reliability, and security."

        calculator = softwareSystem "gRPC-based TCO Cost-Efficiency Calculator" "System that calculates the TCC and TCO of a gRPC-based microservice, its reliability, security, and resiliency tactics and patterns" {

            monolithApp = container "Calculator Service" "Executes the evaluation domain engines, protocol compilations, and cost mapping loops, as well as TCC and TCO calculation." "Java / Spring Boot" "HexagonalCore" {

                thymeleafUi = component "Cost-efficiency calculator UI" "Server-side Thymeleaf views. Presents trade-off matrix visualizations and cost-breakdown graphs." "Thymeleaf Template Engine"
                tcoController = component "TCO Calculator Controller" "Receives .proto IDL via multipart POST, compiles it, measures wire footprints, applies tactics (TLS, JWT), and returns rendered breakdowns." "Spring MVC RestController"
                cloudTcoController = component "Cloud TCO Calculator Controller" "Exposes cached AWS pricing data interfaces (GetProducts) for core services (EC2, ALB, EKS)." "Spring MVC RestController"
                finopsController = component "FinOps Discount Controller" "Exposes savings plan and reserved instance optimization metrics to determine true discount scales." "Spring MVC RestController"
                sessionController = component "Tactics Session Controller" "Maintains ArchitecturalDecisionsDTO in the HTTP session to track current RPS multipliers and security layers." "Spring MVC RestController"
                portfolioController = component "Portfolio ROI Controller" "Exposes aggregated multi-service metrics including ARPU, ROI, and break-even user margins." "Spring MVC RestController"

                costCalculator = component "Cost Calculator Service" "Orchestrates calculations by uniting payload structures, configuration parameters, and live pricing." "Spring Service"
                protoCompiler = component "Protocol Buffer File Compiler" "Compiles uploaded .proto IDLs and measures exact serialized message payload footprints on the wire." "Spring Service"
                tacticMapper = component "Tactics Mapper" "Translates architectural tactics (TLS, JWT, Resiliency options) into quantitative overhead shapes." "Spring Component"
                tacticPopulator = component "Tactics Populator" "Enriches active calculations with required RPS multipliers and security overhead metrics." "Spring Component"

                domainModel = component "Cost Efficiency Calculator Model" "Encapsulates immutable core math evaluating quality attribute trade-offs (Resiliency, Reliability, Security)." "POJO Domain Model"

                cloudPricingPort = component "Cloud Pricing Port" "Outbound contract defining how the core requests infrastructure pricing lines." "Java Interface (SPI)"
                discountPort = component "Discount Port" "Outbound contract defining how the core queries operational cloud adjustments." "Java Interface (SPI)"
                configDataPort = component "Configuration Data Port" "Outbound contract defining how the engine dynamically pulls structural properties." "Java Interface (SPI)"

                awsPricingAdapter = component "AWS Price List Adapter" "Implements CloudPricingPort to fetch rates using GetProducts." "Spring Component (Infra Adapter)"
                awsDiscountAdapter = component "AWS Discount Adapter" "Implements DiscountPort using Price List Query API and Bulk API feeds." "Spring Component (Infra Adapter)"
                propertiesConfigAdapter = component "Properties File Adapter" "Implements Configuration Data Port. Parses externalized property files to treat configuration as data." "Spring Component (Infra Adapter)"
                cloudConfig = component "Cloud Provider Configuration" "Configures HTTP connection pools and SDK clients for AWS calls." "Spring Configuration"
            }
        }

        aws = softwareSystem "Amazon Web Services (AWS)" "Provides cloud pricing data, commitment discounts, savings plans, and APIs for AWS services." "Existing System"

        softwarearchitect -> thymeleafUi "Specifies a gRPC-based microservice, and evaluates its cost-efficiency trade-offs using" "Browser / HTTPS"

        thymeleafUi -> tcoController "Invokes with .proto payload multipart forms" "Internal Call"
        thymeleafUi -> cloudTcoController "Queries for cached service lines" "Internal Call"
        thymeleafUi -> finopsController "Triggers discount projections" "Internal Call"
        thymeleafUi -> sessionController "Updates active architectural choices" "Internal Call"
        thymeleafUi -> portfolioController "Requests composite portfolios" "Internal Call"

        tcoController -> protoCompiler "Requests compilation of"
        tcoController -> tacticMapper "Requests mapping profiles from"
        tcoController -> costCalculator "Triggers core evaluation via"

        finopsController -> costCalculator "Triggers optimization logic within"
        finopsController -> discountPort "Queries discount metrics through"

        sessionController -> tacticPopulator "Saves configuration variables to"
        portfolioController -> costCalculator "Compiles totals through"

        costCalculator -> domainModel "Coordinates"
        costCalculator -> tacticPopulator "Applies multipliers via"
        protoCompiler -> domainModel "Hydrates metrics in"
        tacticPopulator -> tacticMapper "Decodes setups via"

        costCalculator -> configDataPort "Loads runtime parameters, rules, and bounds from"
        propertiesConfigAdapter -> configDataPort "Implements to read configuration from physical data properties"

        costCalculator -> cloudPricingPort "Invokes pricing rules via"

        awsPricingAdapter -> cloudPricingPort "Implements and fulfills"
        awsDiscountAdapter -> discountPort "Implements and fulfills"

        awsPricingAdapter -> cloudConfig "Utilizes clients from"
        awsDiscountAdapter -> cloudConfig "Utilizes clients from"

        cloudConfig -> aws "Retrieves live pricing data, and rate and usage discounts from" "HTTPS"
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
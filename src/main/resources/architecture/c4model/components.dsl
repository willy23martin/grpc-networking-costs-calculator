workspace {

    model {
        architect = person "Software Architect" "Valuates the cost-efficiency of gRPC-based MACH architectures, analyzing trade-offs across resiliency, reliability, and security."

        calculator = softwareSystem "Cost-Efficiency Calculator" "Evaluates and simulates TCO/ROI impact of gRPC serialization footprints, tactics (TLS, JWT), and architectural resiliency patterns." {

            monolithApp = container "Spring Boot Monolith Application" "The single runtime executable managing UI rendering, Hexagonal packages, and data parsing." "Java / Spring Boot" {

                # --- INFRASTRUCTURE LAYER: VIEW ENGINE ---
                thymeleafUi = component "Thymeleaf View Engine" "Renders HTML/JS UI templates server-side within the active Spring Context." "Thymeleaf Component"

                # --- INFRASTRUCTURE LAYER: PRIMARY ADAPTERS (REST Controllers) ---
                tcoController = component "TCO Calculator Controller" "Receives .proto IDL via multipart POST, compiles it, measures wire footprints, applies tactics (TLS, JWT), and returns rendered breakdowns." "Spring MVC RestController"
                cloudTcoController = component "Cloud TCO Calculator Controller" "Exposes cached AWS pricing data interfaces (GetProducts) for core services (EC2, ALB, EKS)." "Spring MVC RestController"
                finopsController = component "FinOps Discount Controller" "Exposes savings plan and reserved instance optimization metrics to determine true discount scales." "Spring MVC RestController"
                sessionController = component "Tactics Session Controller" "Maintains ArchitecturalDecisionsDTO in the HTTP session to track current RPS multipliers and security layers." "Spring MVC RestController"
                portfolioController = component "Portfolio ROI Controller" "Exposes aggregated multi-service metrics including ARPU, ROI, and break-even user margins." "Spring MVC RestController"

                # --- APPLICATION / SERVICE LAYER (Use Cases & Core Services) ---
                costCalculator = component "Cost Calculator Service" "Orchestrates calculations by uniting payload structures, configuration parameters, and live pricing." "Spring Service"
                protoCompiler = component "Protocol Buffer File Compiler" "Compiles uploaded .proto IDLs and measures exact serialized message payload footprints on the wire." "Spring Service"
                tacticMapper = component "Tactics Mapper" "Translates architectural tactics (TLS, JWT, Resiliency options) into quantitative overhead shapes." "Spring Component"
                tacticPopulator = component "Tactics Populator" "Enriches active calculations with required RPS multipliers and security overhead metrics." "Spring Component"

                # --- DOMAIN LAYER (Business Core & Ports) ---
                domainModel = component "Cost Efficiency Calculator Model" "Encapsulates immutable core math evaluating quality attribute trade-offs (Resiliency, Reliability, Security)." "POJO Domain Model"

                # OUTBOUND PORTS (SPI Interfaces)
                cloudPricingPort = component "Cloud Pricing Port" "Outbound contract defining how the core requests infrastructure pricing lines." "Java Interface (SPI)"
                discountPort = component "Discount Port" "Outbound contract defining how the core queries operational cloud adjustments." "Java Interface (SPI)"
                configDataPort = component "Configuration Data Port" "Outbound contract defining how the engine dynamically pulls structural properties." "Java Interface (SPI)"

                # --- INFRASTRUCTURE LAYER: SECONDARY ADAPTERS ---
                awsPricingAdapter = component "AWS Price List Adapter" "Implements CloudPricingPort to fetch rates using GetProducts." "Spring Component (Infra Adapter)"
                awsDiscountAdapter = component "AWS Discount Adapter" "Implements DiscountPort using Price List Query API and Bulk API feeds." "Spring Component (Infra Adapter)"
                propertiesConfigAdapter = component "Properties File Adapter" "Implements Configuration Data Port. Parses externalized property files to treat configuration as data." "Spring Component (Infra Adapter)"
                cloudConfig = component "Cloud Provider Configuration" "Configures HTTP connection pools and SDK clients for AWS calls." "Spring Configuration"
            }
        }

        aws = softwareSystem "Amazon Web Services (AWS)" "Provides cloud pricing data and APIs." "Existing System"

        # --- DATA FLOW RELATIONSHIPS ---
        architect -> thymeleafUi "Submits IDL files and request TCC and TCO calculations" "Browser / HTTPS"

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

        # Configuration as Data injection path
        costCalculator -> configDataPort "Loads runtime parameters, rules, and bounds from"
        propertiesConfigAdapter -> configDataPort "Implements to read configuration from physical data properties"

        costCalculator -> cloudPricingPort "Invokes pricing rules via"

        awsPricingAdapter -> cloudPricingPort "Implements and fulfills"
        awsDiscountAdapter -> discountPort "Implements and fulfills"

        awsPricingAdapter -> cloudConfig "Utilizes clients from"
        awsDiscountAdapter -> cloudConfig "Utilizes clients from"

        cloudConfig -> aws "Executes GetProducts, ListPriceLists, and GetPriceListFileUrl requests" "HTTPS API Call"
    }

    views {
        component monolithApp "ComponentView" "The detailed architecture map including internal Thymeleaf UI, Hexagonal components, Configuration as Data, and external ports." {
            include *
            include architect
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
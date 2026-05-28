workspace {

    model {
        # Actor updated with your exact thesis objective
        architect = person "Software Architect" "Valuates the cost-efficiency of gRPC-based MACH architectures, analyzing trade-offs across resiliency, reliability, and security."

        # System scope updated to reflect trade-off calculation core
        calculator = softwareSystem "Cost-Efficiency Calculator" "Evaluates and simulates TCO/ROI impact of gRPC serialization footprints, tactics (TLS, JWT), and architectural resiliency patterns."

        aws = softwareSystem "Amazon Web Services (AWS)" "Provides cloud pricing data and APIs (EC2, ALB, ElastiCache, Compute Savings Plans)." "Existing System"

        architect -> calculator "Interacts with to model MACH systems and evaluate cost-efficiency trade-offs"
        calculator -> aws "Retrieves live pricing data, and rate and usage discounts from" "HTTPS"
    }

    views {
        systemContext calculator "SystemContext" "System Context diagram for the MACH Cost-Efficiency Evaluation Tool." {
            include *
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
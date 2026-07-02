workspace {

    model {
        softwarearchitect = person "Software Architect" "Person who evaluates the cost-efficiency of a gRPC-based MACH architecture, by analyzing trade-offs across resiliency, reliability, and security"

        calculator = softwareSystem "gRPC-based TCO Cost-Efficiency Calculator" "System that calculates the TCC and TCO of a gRPC-based microservice, its reliability, security, and resiliency tactics and patterns"

        aws = softwareSystem "Amazon Web Services (AWS)" "Provides cloud pricing data, commitment discounts, savings plans, and APIs for AWS services." "Existing System"

        softwarearchitect -> calculator "Specifies a gRPC-based microservice, and evaluates its cost-efficiency trade-offs using"
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
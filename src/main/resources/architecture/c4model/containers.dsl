workspace {

    model {
        softwarearchitect = person "Software Architect" "Valuates the cost-efficiency of gRPC-based MACH architectures, analyzing trade-offs across resiliency, reliability, and security."

        calculator = softwareSystem "gRPC-based TCO Cost-Efficiency Calculator" "System that calculates the TCC and TCO of a gRPC-based microservice, its reliability, security, and resiliency tactics and patterns" {

            monolithApp = container "Calculator Service" "Executes the evaluation domain engines, protocol compilations, and cost mapping loops, as well as TCC and TCO calculation." "Java / Spring Boot" "HexagonalCore"
        }

        aws = softwareSystem "Amazon Web Services (AWS)" "Provides cloud pricing data, commitment discounts, savings plans, and APIs for AWS services." "Existing System"

        softwarearchitect -> monolithApp "Specifies a gRPC-based microservice, and evaluates its cost-efficiency trade-offs using" "Browser / HTTPS"
        monolithApp -> aws "Retrieves live pricing data, and rate and usage discounts from" "HTTPS"
    }

    views {
        container calculator "ContainerView" "The Container layout illustrating the single Spring Boot monolith boundary." {
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
            # Target the custom tag or the default container shape
            element "Container" {
                background #438dd5
                color #ffffff
            }
            # Added style rule to specifically make your Spring Boot container look like a hexagon
            element "HexagonalCore" {
                shape Hexagon
                background #1b4d82
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
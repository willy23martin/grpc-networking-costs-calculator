workspace {

    model {
        architect = person "Software Architect" "Valuates the cost-efficiency of gRPC-based MACH architectures, analyzing trade-offs across resiliency, reliability, and security."

        calculator = softwareSystem "Cost-Efficiency Calculator" "Evaluates and simulates TCO/ROI impact of gRPC serialization footprints, tactics (TLS, JWT), and architectural resiliency patterns." {

            # The UI engine container
            thymeleafUi = container "Thymeleaf Web UI" "Server-side HTML/JS views. Presents trade-off matrix visualizations and cost-breakdown graphs." "Thymeleaf Template Engine"

            # The core execution runtime (This will render as a Hexagon)
            monolithApp = container "Spring Boot Application" "Executes the evaluation domain engines, protocol compilations, and cost mapping loops, as well as TCC and TCO calculation." "Java / Spring Boot" "HexagonalCore"
        }

        aws = softwareSystem "Amazon Web Services (AWS)" "Provides cloud pricing data and APIs." "Existing System"

        # Structural routing paths
        architect -> thymeleafUi "Submits IDL files and request TCC and TCO calculations." "Browser / HTTPS"
        thymeleafUi -> monolithApp "Forwards interaction states and multipart uploads to" "Internal Spring Calls"
        monolithApp -> aws "Pulls service pricing, and service rates and usage discount frames from" "HTTPS"
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
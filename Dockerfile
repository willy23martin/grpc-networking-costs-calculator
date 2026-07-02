FROM eclipse-temurin:21.0.10_7-jdk-ubi10-minimal

LABEL maintainer="William Martin Chavez Gonzalez"

COPY target/gRPCTCONetworkingCostCalculator-1.0-SNAPSHOT.jar gRPCTCONetworkingCostCalculator-1.0-SNAPSHOT.jar

ENTRYPOINT ["java","-jar","/gRPCTCONetworkingCostCalculator-1.0-SNAPSHOT.jar"]
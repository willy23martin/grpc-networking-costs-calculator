package com.calculator.infrastructure.web.rest.helper;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import org.springframework.mock.web.MockMultipartFile;

public class TCOCalculatorControllerTestsHelper {

    public static ArchitecturalDecisionsDTO noTactics(long rps) {
        return noTactics(rps, SecurityTactics.empty());
    }

    public static ArchitecturalDecisionsDTO noTactics(long rps, SecurityTactics security) {
        return new ArchitecturalDecisionsDTO(
                rps,
                new ReliabilityTactics(false, false),
                new TimeoutPattern(false, 0),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                security
        );
    }

    public static MockMultipartFile protoFile(byte[] content) {
        return new MockMultipartFile("protoFile", "order.proto", "text/plain", content);
    }
}

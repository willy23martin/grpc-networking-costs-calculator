package com.calculator.application.services.populator.reliability;

import com.calculator.application.services.mapper.tactics.TacticsMapperService;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReliabilityTacticsPopulatorService {

    @Autowired
    TacticsMapperService tacticsMapperService;

    public void populate(ReliabilityTactics reliabilityTactics, List<Map<String, String>> infoTactics) {
        if (reliabilityTactics.reliabilityClientSideLoadBalancerTactic()) {
            infoTactics.add(tacticsMapperService.tacticEntry("Client-side Load Balancing", null,
                    "Balances the emission of gRPC packets across server instances.", null));
        }
        if (reliabilityTactics.reliabilityServerSideLoadBalancerTactic()) {
            infoTactics.add(tacticsMapperService.tacticEntry("Server-side Load Balancing", null,
                    "Balances the reception of gRPC packets across backend replicas.", null));
        }
    }

}

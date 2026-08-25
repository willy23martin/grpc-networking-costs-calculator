package com.calculator.domain.repository.security;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.cost.NetworkingCost;
import com.calculator.domain.model.cost.networking.NetworkingCostCriteria;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.model.quality.security.SecurityQualityTradeoff;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityArchitecturalDecisionRepositoryTest {

    private final SecurityArchitecturalDecisionRepository repository = new SecurityArchitecturalDecisionRepository();

    @Test
    void getAvailableSecurityDecisions_returnsAllFourTactics() {
        final List<ArchitecturalDecision> decisions = repository.getAvailableSecurityDecisions();

        assertNotNull(decisions);
        assertEquals(4, decisions.size());
        assertEquals("tactic-tls", decisions.get(0).getId());
        assertEquals("tactic-mtls", decisions.get(1).getId());
        assertEquals("tactic-oauth", decisions.get(2).getId());
        assertEquals("tactic-basic-auth", decisions.get(3).getId());
    }

    @Test
    void getTLSTactic_validatesStructureAndTradeoffs() {
        final ArchitecturalDecision decision = repository.getTLSTactic();
        final List<QualityTradeoff> tradeoffs = decision.getArchitecturalCharacteristic().getQualityTradeoffs();

        assertNotNull(decision);
        assertEquals("tactic-tls", decision.getId());
        assertNotNull(decision.getArchitecturalCharacteristic());

        assertEquals(3, tradeoffs.size());

        assertInstanceOf(SecurityQualityTradeoff.class, tradeoffs.get(0));
        assertEquals(TradeoffType.PROMOTES, tradeoffs.get(0).getTradeoffType());
        assertEquals(TradeoffType.INHIBITS, tradeoffs.get(2).getTradeoffType());
        assertEquals("AFFORDABILITY", tradeoffs.get(2).getArchitecturalCharacteristic().getName());
        assertNotNull(decision.getCostFactor());
    }

    @Test
    void getMTLSTactic_validatesStructureAndTradeoffs() {
        final ArchitecturalDecision decision = repository.getMTLSTactic();
        final List<QualityTradeoff> tradeoffs = decision.getArchitecturalCharacteristic().getQualityTradeoffs();

        assertNotNull(decision);
        assertEquals("tactic-mtls", decision.getId());
        assertNotNull(decision.getArchitecturalCharacteristic());

        assertEquals(4, tradeoffs.size());

        assertInstanceOf(SecurityQualityTradeoff.class, tradeoffs.get(0));
        assertEquals("AUTHENTICITY", tradeoffs.get(2).getArchitecturalCharacteristic().getName());
        assertEquals(TradeoffType.INHIBITS, tradeoffs.get(3).getTradeoffType());
        assertEquals("AFFORDABILITY", tradeoffs.get(3).getArchitecturalCharacteristic().getName());
        assertNotNull(decision.getCostFactor());
    }

    @Test
    void getOAuthTactic_validatesStructureAndTradeoffs() {
        final ArchitecturalDecision decision = repository.getOAuthTactic();
        final List<QualityTradeoff> tradeoffs = decision.getArchitecturalCharacteristic().getQualityTradeoffs();

        assertNotNull(decision);
        assertEquals("tactic-oauth", decision.getId());
        assertNotNull(decision.getArchitecturalCharacteristic());

        assertEquals(5, tradeoffs.size());

        assertEquals("ACCOUNTABILITY", tradeoffs.get(1).getArchitecturalCharacteristic().getName());
        assertEquals("NON_REPUDIATION", tradeoffs.get(3).getArchitecturalCharacteristic().getName());
        assertEquals(TradeoffType.INHIBITS, tradeoffs.get(4).getTradeoffType());
        assertEquals("AFFORDABILITY", tradeoffs.get(4).getArchitecturalCharacteristic().getName());
        assertNotNull(decision.getCostFactor());
    }

    @Test
    void getBasicAuthTactic_validatesStructureAndTradeoffs() {
        final ArchitecturalDecision decision = repository.getBasicAuthTactic();
        final List<QualityTradeoff> tradeoffs = decision.getArchitecturalCharacteristic().getQualityTradeoffs();
        final SecurityQualityTradeoff securityTradeoff = (SecurityQualityTradeoff) tradeoffs.getFirst();
        final QualityTradeoff affordabilityTradeoff = tradeoffs.get(1);
        final NetworkingCost networkingCost = (NetworkingCost) decision.getCostFactor();

        assertNotNull(decision);
        assertEquals("tactic-basic-auth", decision.getId());
        assertNotNull(decision.getArchitecturalCharacteristic());

        assertEquals(2, tradeoffs.size());

        assertEquals(TradeoffType.PROMOTES, securityTradeoff.getTradeoffType());
        assertArrayEquals(new String[]{"A02:2021", "A07:2021"}, securityTradeoff.getLinkedOwaspTop10Vulnerabilities());
        assertArrayEquals(new String[]{"Cryptographic Failures", "Identification and Authentication Failures"}, securityTradeoff.getLinkedOwaspLabels());
        assertArrayEquals(new String[]{"CWE-256", "CWE-522", "CWE-287"}, securityTradeoff.getCwes());

        assertEquals(TradeoffType.ORTHOGONAL, affordabilityTradeoff.getTradeoffType());
        assertEquals("AFFORDABILITY", affordabilityTradeoff.getArchitecturalCharacteristic().getName());

        assertInstanceOf(NetworkingCost.class, decision.getCostFactor());

        assertEquals(NetworkingCostCriteria.NONE, networkingCost.getNetworkingCostCriteria());
    }
}
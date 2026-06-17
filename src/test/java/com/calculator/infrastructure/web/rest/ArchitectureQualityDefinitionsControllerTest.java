package com.calculator.infrastructure.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureQualityDefinitionsControllerTest {

    private final ArchitectureQualityDefinitionsController controller = new ArchitectureQualityDefinitionsController();

    @Test
    void getQualityDefinitions_returnsPopulatedCategoriesAndTactics() {
        ResponseEntity<List<ArchitectureQualityDefinitionsController.QualityCategoryDefinition>> response = controller.getQualityDefinitions();
        List<ArchitectureQualityDefinitionsController.QualityCategoryDefinition> categories = response.getBody();

        assertNotNull(categories);
        assertEquals(3, categories.size());

        ArchitectureQualityDefinitionsController.QualityCategoryDefinition reliability = categories.get(0);
        assertEquals("reliability", reliability.categoryId);
        assertEquals("Reliability", reliability.categoryName);
        assertNotNull(reliability.isoDefinition);
        assertNotNull(reliability.seiDefinition);
        assertNotNull(reliability.architectNote);
        assertNotNull(reliability.costRelation);
        assertTrue(reliability.subCharacteristics.length > 0);
        assertTrue(reliability.tactics.length > 0);

        ArchitectureQualityDefinitionsController.TacticGuidance clientLb = reliability.tactics[0];
        assertEquals("tactic-client-lb", clientLb.tacticId);
        assertEquals("Client-side Load Balancing", clientLb.name);
        assertNotNull(clientLb.isoSubCharacteristic);
        assertNotNull(clientLb.briefDefinition);
        assertNotNull(clientLb.whenToApply);
        assertNotNull(clientLb.tradeoff);

        ArchitectureQualityDefinitionsController.QualityCategoryDefinition resiliency = categories.get(1);
        assertEquals("resiliency", resiliency.categoryId);
        assertEquals("Resiliency", resiliency.categoryName);
        assertTrue(resiliency.tactics.length > 0);

        ArchitectureQualityDefinitionsController.QualityCategoryDefinition security = categories.get(2);
        assertEquals("security", security.categoryId);
        assertEquals("Security", security.categoryName);
        assertTrue(security.tactics.length > 0);
    }
}
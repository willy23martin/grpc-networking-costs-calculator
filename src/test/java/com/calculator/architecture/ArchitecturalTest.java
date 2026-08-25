package com.calculator.architecture;

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class ArchitecturalTest {

    @Test
    void services_ShouldNotDependOn_Controllers() {
        ArchRuleDefinition
                .noClasses()
                .that()
                .resideInAPackage("..services..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..rest..");
    }

    @Test
    void services_ShouldEndWithServiceName(){
        ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..services..")
                .should()
                .haveSimpleNameEndingWith("Service");
    }

    @Test
    void controllers_ShouldEndWithNameController_Or_ControllerTest() {
        ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..rest..")
                .should()
                .haveSimpleNameEndingWith("Controller")
                .orShould()
                .haveSimpleNameEndingWith("ControllerTest");
    }

}

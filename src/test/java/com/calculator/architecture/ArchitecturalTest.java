package com.calculator.architecture;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class ArchitecturalTest {

    @Test
    public void services_ShouldNotDependOn_Controllers() {
        ArchRuleDefinition
                .noClasses()
                .that()
                .resideInAPackage("..services..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..rest..");
    }

    @Test
    public void services_ShouldEndWithServiceName(){
        ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..services..")
                .should()
                .haveSimpleNameEndingWith("Service");
    }

    @Test
    public void controllers_ShouldEndWithNameController_Or_ControllerTest() {
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

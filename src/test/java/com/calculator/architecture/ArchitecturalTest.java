package com.calculator.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "com.calculator")
public class ArchitecturalTest {

    @ArchTest
    public static final ArchRule archRule = ArchRuleDefinition
            .noClasses()
            .that()
            .resideInAPackage("..services..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..rest..");

    @ArchTest
    public static final ArchRule archRuleII = ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("..services..")
            .should()
            .haveSimpleNameEndingWith("Service");

    @ArchTest
    public static final ArchRule archRuleIII = ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("..rest..")
            .should()
            .haveSimpleNameEndingWith("Controller")
            .orShould()
            .haveSimpleNameEndingWith("ControllerTest");

}

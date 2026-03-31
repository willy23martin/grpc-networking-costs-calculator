package com.calculator;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/application.services.calculators.rps.security",
        glue = {"com.calculator.application.services.calculators.rps.security"}
)
public class End2EndModelBDDTests {
}

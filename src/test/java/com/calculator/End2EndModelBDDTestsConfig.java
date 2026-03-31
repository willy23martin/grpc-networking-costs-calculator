package com.calculator;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(classes = {
        End2EndModelBDDTests.class
})
public class End2EndModelBDDTestsConfig {
}

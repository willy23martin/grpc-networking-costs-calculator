package com.calculator.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * JUnit 5 suite that wires feature files to their step-definition glue package.
 *
 * WHY THIS CLASS EXISTS:
 * Without a runner, Cucumber cannot discover which feature files to execute
 * or which Java packages contain the step definitions. This class replaces
 * the older JUnit 4 @RunWith(Cucumber.class) approach and works with the
 * cucumber-junit-platform-engine artifact on the classpath.
 *
 * Required Maven/Gradle dependencies (test scope):
 *   - io.cucumber:cucumber-spring
 *   - io.cucumber:cucumber-junit-platform-engine
 *   - org.junit.platform:junit-platform-suite
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.calculator.features")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/cucumber.html, json:target/cucumber-reports/cucumber.json"
)
public class CucumberTestRunner {
}

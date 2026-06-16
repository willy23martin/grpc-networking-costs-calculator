package com.calculator.features;

import io.cucumber.spring.CucumberContextConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberConfiguration {

    @Autowired
    private ApplicationContext ctx;

    @PostConstruct
    public void printMappings() {
        // Run once; prints all registered request mappings to stdout
        RequestMappingHandlerMapping mapping =
                ctx.getBean(RequestMappingHandlerMapping.class);
        mapping.getHandlerMethods().forEach((info, method) ->
                System.out.println("MAPPED: " + info + " -> " + method));
    }
}

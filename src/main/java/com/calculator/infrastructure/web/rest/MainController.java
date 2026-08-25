package com.calculator.infrastructure.web.rest;

import com.calculator.domain.model.architecture.ArchitecturalPattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello world!";
    }

    @GetMapping("/pattern")
    public ArchitecturalPattern obtenerPattern() {
        return ArchitecturalPattern.builder()
                .name("New pattern")
                .build();
    }

}

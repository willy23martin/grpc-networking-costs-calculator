package com.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Logger;

@SpringBootApplication
public class Starter {

    private static final java.util.logging.Logger log = Logger.getLogger(Starter.class.getName());

    public static void main(String[] args) {
        SpringApplication.run(Starter.class, args);
        log.info("gRPC Networking costs calculator has started");
    }

}

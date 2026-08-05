package com.calculator.infrastructure.web.rest;

import com.calculator.Starter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WithMockUser
@SpringBootTest(classes = Starter.class)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
}
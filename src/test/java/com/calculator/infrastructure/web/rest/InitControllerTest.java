package com.calculator.infrastructure.web.rest;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class InitControllerTest {

    @Mock
    private HttpSession session;

    @InjectMocks
    private InitController controller;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    @Test
    void init_PopulatesEmptyBytesAndReturnsCalculatorView() {
        final String view = controller.init(model);

        assertEquals("calculator", view);
        assertEquals("", model.getAttribute("requestMessageBytes"));
        assertEquals("", model.getAttribute("responseMessageBytes"));
    }

}

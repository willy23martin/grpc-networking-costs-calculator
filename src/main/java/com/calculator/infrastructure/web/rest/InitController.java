package com.calculator.infrastructure.web.rest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class InitController implements ErrorController {

    @RequestMapping("/")
    public String init(Model model) {
        model.addAttribute("requestMessageBytes", "");
        model.addAttribute("responseMessageBytes", "");
        return "calculator";
    }

}

package com.coronatracker.controller;

import com.coronatracker.service.CoronaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CoronaController {

    private final CoronaService coronaService;

    @GetMapping("/")
    public String hello(Model model) {
        model.addAttribute("stats", coronaService.getStats());
        model.addAttribute("sum", coronaService.getTotalCases());
        model.addAttribute("sumOfNew", coronaService.getNewCases());
        return "index";
    }
}

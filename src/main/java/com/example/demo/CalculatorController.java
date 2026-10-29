package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalculatorController {

    private final CalculatorService calculatorService;

    // Spring passes in the CalculatorService automatically
    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    // Runs every time a button is clicked.
    // Example: http://localhost:8080/press?expression=7%2B&key=8
    @GetMapping("/press")
    public OperationModel press(OperationModel operationModel) {
        try {
            calculatorService.press(operationModel);
        } catch (IllegalArgumentException e) {
            operationModel.setError("Invalid key");   // a button we don't recognise
        }
        return operationModel;   // Spring Boot sends it back as JSON automatically
    }
}
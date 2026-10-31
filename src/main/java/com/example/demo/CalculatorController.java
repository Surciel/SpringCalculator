package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @GetMapping("/press")
    public OperationModel press(OperationModel operationModel) {
        try {
            calculatorService.press(operationModel);
        } catch (IllegalArgumentException e) {
            operationModel.setError("Invalid key");
        }
        return operationModel;
    }
}
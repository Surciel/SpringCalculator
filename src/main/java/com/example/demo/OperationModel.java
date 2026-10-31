package com.example.demo;

import java.util.Arrays;

public class OperationModel {
    public enum Key {
        // yung model pala nito based sa samsung calculator
        // also, ganito siya para readable yung sa switch statements natin trust trust

        // numeros
        ZERO("0"), ONE("1"), TWO("2"), THREE("3"), FOUR("4"),
        FIVE("5"), SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"),

        // decimal
        DECIMAL("."),

        // yung arithmetic operators (si MDAS)
        ADD("+"), SUBTRACT("-"), MULTIPLY("×"), DIVIDE("÷"),

        // other keys
        PERCENT("%"),
        PARENTHESES("()"),
        PLUS_MINUS("+/-"),
        CLEAR("C"),
        BACKSPACE("back"),
        EQUALS("=");

        private final String symbol;

        Key(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static Key fromSymbol(String symbol) {
            return Arrays.stream(values())
                    .filter(key -> key.symbol.equals(symbol))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown Key: " + symbol));
        }
    }

    private String expression = ""; // pang handle ng expression display (like pinapakita ano combo mo right niau)
    private String key; // symbol kung anong button pinindot
    private String error; // error message (alalahanin mo ito i will return to this)

    // setter getter na to boseng
    public Key pressedKey() {
        return Key.fromSymbol(key);
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
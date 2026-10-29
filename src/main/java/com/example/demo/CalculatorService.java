package com.example.demo;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.example.demo.OperationModel.Key;
import org.springframework.stereotype.Service;

@Service
public class CalculatorService {
    private static final String OPERATORS = "+-×÷";

    public void press(OperationModel model) {
        model.setError(null);
        String expr = model.getExpression() == null ? "" : model.getExpression();
        Key key = model.pressedKey();

        switch (key) { // tae dapat ito pala ginamit ko noong quiz
            case ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE -> expr = addDigit(expr, key.getSymbol());
            case DECIMAL -> expr = addDecimal(expr);
            case ADD, SUBTRACT, MULTIPLY, DIVIDE -> expr = addOperator(expr, key.getSymbol());
            case PERCENT -> expr = endsWithValue(expr) ? expr + "%" : expr;
            case PARENTHESES -> expr = addParenthesis(expr);
            case PLUS_MINUS -> expr = toggleSign(expr);
            case CLEAR -> expr = "";
            case BACKSPACE -> expr = expr.isEmpty() ? expr : expr.substring(0, expr.length() - 1);
            case EQUALS -> {
                if (!expr.isEmpty()) {
                    try {
                        expr = evaluate(expr);
                    } catch (IllegalArgumentException e) {
                        model.setError(e.getMessage());
                    }
                }
            }

        }
        model.setExpression(expr);
    }

    // keypard behavior na ito sa baba

    private String addDigit(String expr, String digit) {
        if (expr.endsWith(")") || expr.endsWith("%")) {
            return expr + "x" + digit; // validation, basically parenthesis w/o operator after reads as multiply
        }
        if (lastNumber(expr).equals("0")) {
            return expr.substring(0, expr.length() - 1) + digit;
        }
        return expr + digit;
    }

    private String addDecimal(String expr) {
        String number = lastNumber(expr);
        if (!number.isEmpty()) {
            return number.contains(".") ? expr : expr + "."; // one decimal lang per number;
        }
        if (expr.endsWith(")") || expr.endsWith("%")) {
            return expr + "x0.";
        }
        return expr + "0.";
    }

    private String addOperator(String expr, String operator) {
        if (expr.isEmpty() || expr.endsWith("(")) {
            return operator.equals("-") ? expr + operator : expr; // ito yung pag allow ng minus (yung +/-) na magstart instead of subtract
        }
        char last = expr.charAt(expr.length() - 1);
        if (OPERATORS.indexOf(last) >= 0) {
            String base = expr.substring(0, expr.length() - 1);
            return (base.isEmpty() || base.endsWith("(")) ? expr : base + operator;
        }
        return expr + operator;
    }

    private String addParenthesis(String expr) {
        if (!endsWithValue(expr)) {
            return expr + "(";
        }
        return countOf(expr, '(') > countOf(expr, ')') ? expr + ")" : expr + "x(";
    }

    private String toggleSign(String expr) {
        if (expr.endsWith(")")) {
            String inner = expr.substring(0, expr.length() - 1);
            int start = lastNumberStart(inner);
            if (start > 1 && start < inner.length() && inner.startsWith("(-", start - 2)) {
                return inner.substring(0, start - 2) + inner.substring(start);
            }
            return expr;
        }
        int start = lastNumberStart(expr);
        if (start < expr.length()) {
            return expr.substring(0 ,start) + "(-" + expr.substring(start) + ")";
        }
        return expr;
    }

    private boolean endsWithValue(String expr) {
        if (expr.isEmpty()) {
            return false;
        }
        char last = expr.charAt(expr.length() - 1);
        return Character.isDigit(last) || last == ')' || last == '%';
    }

    private int lastNumberStart(String expr) {
        int i = expr.length();
        while (i > 0 && (Character.isDigit(expr.charAt(i - 1)) || expr.charAt(i - 1) == '.')) {
            i--;
        }
        return i;
    }

    private String lastNumber(String expr) {
        return expr.substring(lastNumberStart(expr));
    }

    private long countOf(String text, char c) {
        return text.chars().filter(ch -> ch == c). count();
    }

    // ito for evaluation na (tinamad nakong mag comment)
    private String evaluate(String expr) {
        long missing = Math.max(0, countOf(expr, '(') - countOf(expr, ')'));
        String text = expr + ")".repeat((int) missing);

        return format(new Parser(text).parse());
    }

    private String format (double value) {
        DecimalFormat format = new DecimalFormat("0.##########", DecimalFormatSymbols.getInstance(Locale.ROOT));
        String text = format.format(value);
        return text.equals("-0") ? "0" : text;
    }

    private static final class Parser {
        private final String text;
        private int pos = 0;

        Parser(String text) {
            this.text = text;
        }

        private IllegalArgumentException invalid() {
            return new IllegalArgumentException("Invalid format");
        }

        double parse() {
            double value = expression();
            if (pos < text.length()) {
                throw invalid();
            }
            return value;
        }

    private double expression() {
        double value = term();
        while (peek('+') || peek('-')) {
            char operator = text.charAt(pos++);
            double right = term();
            value = operator == '+' ? value + right : value - right;
        }
        return value;
    }

    private double term() {
        double value = unary();
        while (peek('×') || peek('÷')) {
            char operator = text.charAt(pos++);
            double right = unary();
            if (operator == '×') {
                value = value * right;
            } else {
                if (right == 0) {
                    throw new IllegalArgumentException("Can't divide by 0");
                }
                value = value / right;
            }
        }
        return value;
    }

    private double unary() {
        if (peek('-')) {
            pos++;
            return -unary();
        }
        return percent();
    }

    private double percent() {
        double value = primary();
        while (peek('%')) {
            pos++;
            value = value / 100;
        }
        return value;
    }

    private double primary() {
        if (peek('(')) {
            pos++;
            double value = expression();
            if (!peek(')')) {
                throw invalid();
            }
            pos++;
            return value;
        }
        return number();
    }

    private double number() {
        int start = pos;
        while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) {
            pos++;
        }
        try {
            return Double.parseDouble(text.substring(start, pos));
        } catch (NumberFormatException e) {
            throw invalid();
        }
    }

    private boolean peek(char c) {
        return pos < text.length() && text.charAt(pos) == c;
        }
    }
}
package com.example.demo;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import com.example.demo.OperationModel.Key;
import org.springframework.stereotype.Service;

@Service
public class CalculatorService {
    private static final String OPERATORS = "+-×÷x";

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
            return expr + "×" + digit; // validation, basically parenthesis w/o operator before or after reads as multiply
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
            return expr + "×0.";
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
        return countOf(expr, '(') > countOf(expr, ')') ? expr + ")" : expr + "×(";
    }

    private String toggleSign(String expr) { //ito yung toggle sign yung magic number na pindutin mo- expression magiging nega or posi
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

    private boolean endsWithValue(String expr) { //eprcentage yehs
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

    // below related lahat sa parentheis
    private String evaluate(String expr) {
        // x = *
        String text = expr.replace('x', '×');

        // pang automatic closing ng brakcet kagaya sa phone
        long missing = countOf(text, '(') - countOf(text, ')');
        for (int n = 0; n < missing; n++) {
            text = text + ")";
        }

        // first parnethsis first (basta yung unang ( and unang aanuhin ng una))
        while (text.contains("(")) {
            int open = text.lastIndexOf('(');
            int close = text.indexOf(')', open);
            if (close == -1) {
                throw new IllegalArgumentException("??? so ano maglalagay ka lang ( tapos ayon na? wala man lang expression buseng");
            }

            // someting something bracket near operator
            if (open > 0 && "(+-×÷".indexOf(text.charAt(open - 1)) < 0) {
                throw new IllegalArgumentException("OY BAWAL YAN");
            }
            if (close + 1 < text.length() && ")+-×÷%".indexOf(text.charAt(close + 1)) < 0) {
                throw new IllegalArgumentException("BAWAL NGA, IBA TRY MO KULIT");
            }

            String inside = text.substring(open + 1, close);
            double answer = solveSimple(inside);
            text = text.substring(0, open) + plain(answer) + text.substring(close + 1);
        }

        // if no bracket left, sovle lahat
        return format(solveSimple(text));
    }

    // ito for simplistic reading or solving (yung walang paretnhsis)
    private double solveSimple(String text) {
        ArrayList<Double> numbers = new ArrayList<>();
        ArrayList<Character> operators = new ArrayList<>();

        // nubmers and operators
        int pos = 0;
        while (true) {
            // minus signs in front of a number (two minuses cancel out)
            boolean negative = false;
            while (pos < text.length() && text.charAt(pos) == '-') {
                negative = !negative;
                pos++;
            }

            // the number itself
            int start = pos;
            while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("ayan mali, ayos bos");
            }

            double number;
            try {
                number = Double.parseDouble(text.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("???? ayan mali tuloy");
            }

            // each % divides by 100
            while (pos < text.length() && text.charAt(pos) == '%') {
                number = number / 100;
                pos++;
            }

            if (negative) {
                number = -number;
            }
            numbers.add(number);

            // pinaka end ng text, meaning tapos na magbasa
            if (pos >= text.length()) {
                break;
            }

            // the operator after the number
            char op = text.charAt(pos);
            if (op != '+' && op != '-' && op != '×' && op != '÷') {
                throw new IllegalArgumentException("ano ba ha, napapagod nako jo!");
            }
            operators.add(op);
            pos++;
        }

        // ito yung rule ng mdas (multiplication division then addition subtraction left to right reaidng)
        int index = 0;
        while (index < operators.size()) {
            char op = operators.get(index);

            if (op == '×' || op == '÷') {
                double left = numbers.get(index);
                double right = numbers.get(index + 1);
                double result;

                switch (op) {
                    case '×':
                        result = left * right;
                        break;
                    case '÷':
                        if (right == 0) {
                            throw new IllegalArgumentException("neng, kailan ka nakakaita na pwede mag divide by 0??");
                        }
                        result = left / right;
                        break;
                    default:
                        throw new IllegalArgumentException("smh tinatamad nako magsulat ng kada sariling argument, ulitin mo nalang");
                }

                // replace two numbers with result and remove operator
                numbers.set(index, result);
                numbers.remove(index + 1);
                operators.remove(index);
            } else {
                index++;
            }
        }

        // ito na yung sa AS ng MDAS (same process asbefore, L>r)
        double answer = numbers.get(0);
        for (int n = 0; n < operators.size(); n++) {
            char op = operators.get(n);
            double next = numbers.get(n + 1);

            switch (op) {
                case '+':
                    answer = answer + next;
                    break;
                case '-':
                    answer = answer - next;
                    break;
                default:
                    throw new IllegalArgumentException("LT nagkamali kapa dito");
            }
        }
        return answer;
    }

    // (OLIVE DELIGHTS), lahat ng mga decimals- kaya yung bracket answer indi rounded before siya magamit ulit
    // also yung root pang read as US or kung ano mang native na decimal format ang ginagamit (same with numeric system; iba iba kasi sa ibang basna)
    private String plain(double value) {
        DecimalFormat plain = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        plain.setMaximumFractionDigits(340);
        return plain.format(value);
    }

    // final answer; up to 10 decimal tapos wala nang extra zero bs
    private String format(double value) {
        DecimalFormat format = new DecimalFormat("0.##########", DecimalFormatSymbols.getInstance(Locale.ROOT));
        String text = format.format(value);
        return text.equals("-0") ? "0" : text;
    }
}
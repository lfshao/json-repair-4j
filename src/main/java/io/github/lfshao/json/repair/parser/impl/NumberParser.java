package io.github.lfshao.json.repair.parser.impl;

import io.github.lfshao.json.repair.core.JsonContext;
import io.github.lfshao.json.repair.core.JsonContext.ContextValues;
import io.github.lfshao.json.repair.core.JsonParser;
import io.github.lfshao.json.repair.parser.JsonElementParser;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * 数字解析器
 */
public class NumberParser implements JsonElementParser {

    private static final Set<Character> NUMBER_CHARS = new HashSet<>();

    static {
        for (char c : "0123456789-.eE/,".toCharArray()) {
            NUMBER_CHARS.add(c);
        }
    }

    private final JsonParser parser;

    public NumberParser(JsonParser parser) {
        this.parser = parser;
    }

    @Override
    public Object parse() {
        return parseNumber();
    }

    @Override
    public boolean accept(Character ch, JsonContext context) {
        return !context.isEmpty() && ch != null && (Character.isDigit(ch) || ch == '-' || ch == '.');
    }

    public Object parseNumber() {
        // <number> is a valid real number expressed in one of a number of given formats
        StringBuilder numberStr = new StringBuilder();
        Character ch = parser.getCharAt(0);
        boolean isArray = parser.getContext().getCurrent() == ContextValues.ARRAY;

        while (ch != null && NUMBER_CHARS.contains(ch) && (!isArray || ch != ',')) {
            numberStr.append(ch);
            parser.setIndex(parser.getIndex() + 1);
            ch = parser.getCharAt(0);
        }

        if (numberStr.length() > 0 &&
                (numberStr.charAt(numberStr.length() - 1) == '-' ||
                        numberStr.charAt(numberStr.length() - 1) == 'e' ||
                        numberStr.charAt(numberStr.length() - 1) == 'E' ||
                        numberStr.charAt(numberStr.length() - 1) == '/' ||
                        numberStr.charAt(numberStr.length() - 1) == ',')) {
            // The number ends with a non valid character for a number/currency, rolling back one
            numberStr.setLength(numberStr.length() - 1);
            parser.setIndex(parser.getIndex() - 1);
        } else if (ch != null && Character.isLetter(ch)) {
            // this was a string instead, sorry
            parser.setIndex(parser.getIndex() - numberStr.length());
            return parser.parseString();
        }

        try {
            String numString = numberStr.toString();
            if (numString.contains(",")) {
                return numString;
            }
            if (numString.contains(".") || numString.contains("e") || numString.contains("E")) {
                return Double.parseDouble(numString);
            } else {
                // Try to parse as int first, then long, then BigInteger for very large numbers
                try {
                    return Integer.parseInt(numString);
                } catch (NumberFormatException e1) {
                    try {
                        return Long.parseLong(numString);
                    } catch (NumberFormatException e2) {
                        // For very large integers, use BigInteger like Python's unlimited int
                        return new BigInteger(numString);
                    }
                }
            }
        } catch (NumberFormatException e) {
            return numberStr.toString();
        }
    }
} 
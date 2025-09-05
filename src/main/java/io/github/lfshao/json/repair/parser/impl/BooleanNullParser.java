package io.github.lfshao.json.repair.parser.impl;

import io.github.lfshao.json.repair.core.JsonContext;
import io.github.lfshao.json.repair.core.JsonParser;
import io.github.lfshao.json.repair.parser.JsonElementParser;

/**
 * 布尔值和null解析器
 */
public class BooleanNullParser implements JsonElementParser {

    private final JsonParser parser;

    public BooleanNullParser(JsonParser parser) {
        this.parser = parser;
    }

    @Override
    public Object parse() {
        return parseBooleanOrNull();
    }

    @Override
    public boolean accept(Character ch, JsonContext context) {
        // BooleanNullParser通常由StringParser内部调用，不直接由主解析器调用
        return false;
    }

    public Object parseBooleanOrNull() {
        // <boolean> is one of the literal strings 'true', 'false', or 'null' (unquoted)
        int startingIndex = parser.getIndex();
        Character ch = parser.getCharAt();
        String charLower = ch != null ? ch.toString().toLowerCase() : "";

        String targetWord = null;
        Object returnValue = null;

        if ("t".equals(charLower)) {
            targetWord = "true";
            returnValue = true;
        } else if ("f".equals(charLower)) {
            targetWord = "false";
            returnValue = false;
        } else if ("n".equals(charLower)) {
            targetWord = "null";
            returnValue = null;
        }

        if (targetWord != null) {
            int i = 0;
            while (ch != null && i < targetWord.length() &&
                    charLower.equals(String.valueOf(targetWord.charAt(i)))) {
                i++;
                parser.setIndex(parser.getIndex() + 1);
                ch = parser.getCharAt();
                charLower = ch != null ? ch.toString().toLowerCase() : "";
            }
            if (i == targetWord.length()) {
                return returnValue;
            }
        }

        // If nothing works reset the index before returning
        parser.setIndex(startingIndex);
        return "";
    }
} 
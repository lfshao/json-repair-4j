package io.github.lfshao.json.repair.parsers;

import io.github.lfshao.json.repair.JsonParser;

/**
 * 布尔值和null解析器
 */
public class BooleanNullParser {

    private final JsonParser parser;

    public BooleanNullParser(JsonParser parser) {
        this.parser = parser;
    }

    public Object parseBooleanOrNull() {
        // <boolean> is one of the literal strings 'true', 'false', or 'null' (unquoted)
        int startingIndex = parser.getIndex();
        Character ch = parser.getCharAt(0);
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
                ch = parser.getCharAt(0);
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
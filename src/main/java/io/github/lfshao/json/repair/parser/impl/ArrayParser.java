package io.github.lfshao.json.repair.parser.impl;

import io.github.lfshao.json.repair.core.JsonContext;
import io.github.lfshao.json.repair.core.JsonContext.ContextValues;
import io.github.lfshao.json.repair.core.JsonParser;
import io.github.lfshao.json.repair.core.ObjectComparer;
import io.github.lfshao.json.repair.parser.JsonElementParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 数组解析器
 */
public class ArrayParser implements JsonElementParser {

    private final JsonParser parser;

    public ArrayParser(JsonParser parser) {
        this.parser = parser;
    }

    @Override
    public Object parse() {
        return parseArray();
    }

    @Override
    public boolean accept(Character ch, JsonContext context) {
        return ch != null && ch == '[';
    }

    public List<Object> parseArray() {
        List<Object> arr = new ArrayList<>();
        parser.getContext().set(ContextValues.ARRAY);

        Character ch = parser.getCharAt(0);
        while (ch != null && ch != ']' && ch != '}') {
            parser.skipWhitespacesAt();
            Object value = "";

            if (ch != null && JsonParser.STRING_DELIMITERS.contains(ch)) {
                // Sometimes it can happen that LLMs forget to start an object and then you think it's a string in an array
                // So we are going to check if this string is followed by a : or not
                // And either parse the string or parse the object
                int i = 1;
                i = parser.skipToCharacter(ch, i);
                i = parser.skipWhitespacesAt(i + 1, false);
                Character nextChar = parser.getCharAt(i);
                if (nextChar != null && nextChar == ':') {
                    value = new ObjectParser(parser).parseObject();
                } else {
                    value = new StringParser(parser).parseString();
                }
            } else {
                value = parser.parseJson();
            }

            // It is possible that parseJson() returns nothing valid, so we increase by 1
            if (ObjectComparer.isStrictlyEmpty(value)) {
                parser.setIndex(parser.getIndex() + 1);
            } else if ("...".equals(value) && parser.getCharAt(-1) != null && parser.getCharAt(-1) == '.') {
                parser.log("While parsing an array, found a stray '...'; ignoring it");
            } else {
                arr.add(value);
            }

            // skip over whitespace after a value but before closing ]
            ch = parser.getCharAt(0);
            while (ch != null && ch != ']' && (Character.isWhitespace(ch) || ch == ',')) {
                parser.setIndex(parser.getIndex() + 1);
                ch = parser.getCharAt(0);
            }
        }

        // Especially at the end of an LLM generated json you might miss the last "]"
        if (ch != null && ch != ']') {
            parser.log("While parsing an array we missed the closing ], ignoring it");
        }

        parser.setIndex(parser.getIndex() + 1);
        parser.getContext().reset();
        return arr;
    }
} 
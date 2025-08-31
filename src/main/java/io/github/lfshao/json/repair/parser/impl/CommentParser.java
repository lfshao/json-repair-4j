package io.github.lfshao.json.repair.parser.impl;

import io.github.lfshao.json.repair.core.JsonContext;
import io.github.lfshao.json.repair.core.JsonContext.ContextValues;
import io.github.lfshao.json.repair.core.JsonParser;
import io.github.lfshao.json.repair.parser.JsonElementParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 注释解析器
 */
public class CommentParser implements JsonElementParser {

    private final JsonParser parser;

    public CommentParser(JsonParser parser) {
        this.parser = parser;
    }

    @Override
    public Object parse() {
        return parseComment();
    }

    @Override
    public boolean accept(Character ch, JsonContext context) {
        return ch != null && (ch == '#' || ch == '/');
    }

    public Object parseComment() {
        // Parse code-like comments:
        // - "# comment": A line comment that continues until a newline.
        // - "// comment": A line comment that continues until a newline.
        // - "/* comment */": A block comment that continues until the closing delimiter "*/".
        // The comment is skipped over and an empty string is returned so that comments do not interfere
        // with the actual JSON elements.

        Character ch = parser.getCharAt(0);
        List<Character> terminationCharacters = new ArrayList<>(Arrays.asList('\n', '\r'));

        if (parser.getContext().contains(ContextValues.ARRAY)) {
            terminationCharacters.add(']');
        }
        if (parser.getContext().contains(ContextValues.OBJECT_VALUE)) {
            terminationCharacters.add('}');
        }
        if (parser.getContext().contains(ContextValues.OBJECT_KEY)) {
            terminationCharacters.add(':');
        }

        // Line comment starting with #
        if (ch != null && ch == '#') {
            StringBuilder comment = new StringBuilder();
            while (ch != null && !terminationCharacters.contains(ch)) {
                comment.append(ch);
                parser.setIndex(parser.getIndex() + 1);
                ch = parser.getCharAt(0);
            }
            parser.log("Found line comment: " + comment + ", ignoring");
        }
        // Comments starting with '/'
        else if (ch != null && ch == '/') {
            Character nextChar = parser.getCharAt(1);
            // Handle line comment starting with //
            if (nextChar != null && nextChar == '/') {
                StringBuilder comment = new StringBuilder("//");
                parser.setIndex(parser.getIndex() + 2); // Skip both slashes.
                ch = parser.getCharAt(0);
                while (ch != null && !terminationCharacters.contains(ch)) {
                    comment.append(ch);
                    parser.setIndex(parser.getIndex() + 1);
                    ch = parser.getCharAt(0);
                }
                parser.log("Found line comment: " + comment + ", ignoring");
            }
            // Handle block comment starting with /*
            else if (nextChar != null && nextChar == '*') {
                StringBuilder comment = new StringBuilder("/*");
                parser.setIndex(parser.getIndex() + 2); // Skip '/*'
                while (true) {
                    ch = parser.getCharAt(0);
                    if (ch == null) {
                        parser.log("Reached end-of-string while parsing block comment; unclosed block comment.");
                        break;
                    }
                    comment.append(ch);
                    parser.setIndex(parser.getIndex() + 1);
                    if (comment.toString().endsWith("*/")) {
                        break;
                    }
                }
                parser.log("Found block comment: " + comment + ", ignoring");
            } else {
                // Skip standalone '/' characters that are not part of a comment
                // to avoid getting stuck in an infinite loop
                parser.setIndex(parser.getIndex() + 1);
            }
        }

        if (parser.getContext().isEmpty()) {
            return parser.parseJson();
        } else {
            return "";
        }
    }
} 
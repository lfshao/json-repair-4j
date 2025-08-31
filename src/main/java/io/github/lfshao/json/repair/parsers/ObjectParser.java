package io.github.lfshao.json.repair.parsers;

import io.github.lfshao.json.repair.JsonContext.ContextValues;
import io.github.lfshao.json.repair.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对象解析器
 */
public class ObjectParser {

    private final JsonParser parser;

    public ObjectParser(JsonParser parser) {
        this.parser = parser;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseObject() {
        // <object> ::= '{' [ <member> *(', ' <member>) ] '}' ; A sequence of 'members'
        Map<String, Object> obj = new LinkedHashMap<>();

        Character ch = parser.getCharAt(0);
        while ((ch != null ? ch : '}') != '}') {

            // This is what we expect to find:
            // <member> ::= <string> ': ' <json>

            // Skip filler whitespaces
            parser.skipWhitespacesAt();

            // Sometimes LLMs do weird things, if we find a ":" so early, we'll change it to "," and move on
            ch = parser.getCharAt(0);
            if (ch != null && ch.equals(':')) {
                parser.log("While parsing an object we found a : before a key, ignoring");
                parser.setIndex(parser.getIndex() + 1);
            }

            // We are now searching for they string key
            // Context is used in the string parser to manage the lack of quotes
            parser.getContext().set(ContextValues.OBJECT_KEY);

            // Save this index in case we need find a duplicate key
            int rollbackIndex = parser.getIndex();

            // <member> starts with a <string>
            String key = "";
            while (parser.getCharAt(0) != null) {
                // The rollback index needs to be updated here in case the key is empty
                rollbackIndex = parser.getIndex();
                if (parser.getCharAt(0) != null && parser.getCharAt(0) == '[' && key.isEmpty()) {
                    // Is this an array?
                    // Need to check if the previous parsed value contained in obj is an array and in that case parse and merge the two
                    String prevKey = null;
                    if (!obj.isEmpty()) {
                        // Get the last key
                        List<String> keys = new ArrayList<>(obj.keySet());
                        prevKey = keys.get(keys.size() - 1);
                    }

                    if (prevKey != null && obj.get(prevKey) instanceof List) {
                        // If the previous key's value is an array, parse the new array and merge
                        parser.setIndex(parser.getIndex() + 1);
                        List<Object> newArray = new ArrayParser(parser).parseArray();
                        if (newArray != null) {
                            // Merge and flatten the arrays
                            Object prevValue = obj.get(prevKey);
                            if (prevValue instanceof List) {
                                List<Object> prevList = (List<Object>) prevValue;
                                if (newArray.size() == 1 && newArray.get(0) instanceof List) {
                                    prevList.addAll((List<Object>) newArray.get(0));
                                } else {
                                    prevList.addAll(newArray);
                                }
                            }
                            parser.skipWhitespacesAt();
                            if (parser.getCharAt(0) != null && parser.getCharAt(0) == ',') {
                                parser.setIndex(parser.getIndex() + 1);
                            }
                            parser.skipWhitespacesAt();
                            continue;
                        }
                    }
                }

                Object keyResult = new StringParser(parser).parseString();
                key = keyResult != null ? keyResult.toString() : "";

                if (key.isEmpty()) {
                    parser.skipWhitespacesAt();
                }

                ch = parser.getCharAt(0);
                if (!key.isEmpty() || (key.isEmpty() && ch != null && (ch == ':' || ch == '}'))) {
                    // If the string is empty but there is a object divider, we are done here
                    break;
                }
            }

            if (parser.getContext().contains(ContextValues.ARRAY) && obj.containsKey(key)) {
                parser.log("While parsing an object we found a duplicate key, closing the object here and rolling back the index");
                parser.setIndex(rollbackIndex - 1);
                // add an opening curly brace to make this work
                String jsonStr = parser.getJsonStr();
                parser.setJsonStr(jsonStr.substring(0, parser.getIndex() + 1) + "{" + jsonStr.substring(parser.getIndex() + 1));
                break;
            }

            // Skip filler whitespaces
            parser.skipWhitespacesAt();

            // We reached the end here
            ch = parser.getCharAt(0);
            if (ch == null || ch.equals('}')) {
                continue;
            }

            parser.skipWhitespacesAt();

            // An extreme case of missing ":" after a key
            ch = parser.getCharAt(0);
            if (ch == null || !ch.equals(':')) {
                parser.log("While parsing an object we missed a : after a key");
            }

            parser.setIndex(parser.getIndex() + 1);
            parser.getContext().reset();
            parser.getContext().set(ContextValues.OBJECT_VALUE);

            // The value can be any valid json
            parser.skipWhitespacesAt();

            // Corner case, a lone comma
            Object value = "";
            ch = parser.getCharAt(0);
            if (ch != null && (ch == ',' || ch == '}')) {
                parser.log("While parsing an object value we found a stray , ignoring it");
            } else {
                value = parser.parseJson();
            }

            // Reset context since our job is done
            parser.getContext().reset();
            obj.put(key, value);

            ch = parser.getCharAt(0);
            if (ch != null && (ch == ',' || ch == '\'' || ch == '"')) {
                parser.setIndex(parser.getIndex() + 1);
            }

            // Remove trailing spaces
            parser.skipWhitespacesAt();

        }

        // 跳过 '}'
        parser.setIndex(parser.getIndex() + 1);
        return obj;
    }
} 
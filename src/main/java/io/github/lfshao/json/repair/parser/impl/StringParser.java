package io.github.lfshao.json.repair.parser.impl;

import io.github.lfshao.json.repair.core.JsonContext;
import io.github.lfshao.json.repair.core.JsonContext.ContextValues;
import io.github.lfshao.json.repair.core.JsonParser;
import io.github.lfshao.json.repair.parser.JsonElementParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 字符串解析器 - 完全对应Python版本parse_string.py的逻辑
 */
public class StringParser implements JsonElementParser {

    private final JsonParser parser;

    public StringParser(JsonParser parser) {
        this.parser = parser;
    }

    @Override
    public Object parse() {
        return parseString();
    }

    @Override
    public boolean accept(Character ch, JsonContext context) {
        return !context.isEmpty() && (JsonParser.STRING_DELIMITERS.contains(ch) || Character.isLetter(ch));
    }

    public Object parseString() {
        // <string> is a string of valid characters enclosed in quotes
        // i.e. { name: "John" }
        // Somehow all weird cases in an invalid JSON happen to be resolved in this function, so be careful here

        // Flag to manage corner cases related to missing starting quote
        boolean missingQuotes = false;
        boolean doubledQuotes = false;
        char lstringDelimiter = '"';
        char rstringDelimiter = '"';

        Character ch = parser.getCharAt();
        if (ch != null && (ch == '#' || ch == '/')) {
            return new CommentParser(parser).parseComment();
        }

        // A valid string can only start with a valid quote or, in our case, with a literal
        while (ch != null && !JsonParser.STRING_DELIMITERS.contains(ch) && !Character.isLetterOrDigit(ch)) {
            parser.setIndex(parser.getIndex() + 1);
            ch = parser.getCharAt();
        }

        if (ch == null) {
            // This is an empty string
            return "";
        }

        // Ensuring we use the right delimiter
        if (ch == '\'') {
            lstringDelimiter = rstringDelimiter = '\'';
        } else if (ch == '“') {
            lstringDelimiter = '“';
            rstringDelimiter = '”';
        } else if (Character.isLetterOrDigit(ch)) {
            // This could be a <boolean> and not a string. Because (T)rue or (F)alse or (N)ull are valid
            // But remember, object keys are only of type string
            if ((ch.toString().equalsIgnoreCase("t") || ch.toString().equalsIgnoreCase("f") ||
                    ch.toString().equalsIgnoreCase("n")) &&
                    parser.getContext().getCurrent() != ContextValues.OBJECT_KEY) {
                Object value = new BooleanNullParser(parser).parseBooleanOrNull();
                if (!"".equals(value)) {
                    return value;
                }
            }
            parser.log("While parsing a string, we found a literal instead of a quote");
            missingQuotes = true;
        }

        if (!missingQuotes) {
            parser.setIndex(parser.getIndex() + 1);
        }

        // There is sometimes a weird case of doubled quotes, we manage this also later in the while loop
        if (parser.getCharAt() != null && JsonParser.STRING_DELIMITERS.contains(parser.getCharAt()) &&
                parser.getCharAt() == lstringDelimiter) {
            // If it's an empty key, this was easy
            Character nextChar = parser.getCharAt(1);
            if ((parser.getContext().getCurrent() == ContextValues.OBJECT_KEY && nextChar != null && nextChar == ':') ||
                    (parser.getContext().getCurrent() == ContextValues.OBJECT_VALUE && nextChar != null && (nextChar == ',' || nextChar == '}'))) {
                parser.setIndex(parser.getIndex() + 1);
                return "";
            } else if (parser.getCharAt(1) != null && parser.getCharAt(1) == lstringDelimiter) {
                // There's something fishy about this, we found doubled quotes and then again quotes
                parser.log("While parsing a string, we found a doubled quote and then a quote again, ignoring it");
                return "";
            }

            // Find the next delimiter
            int i = parser.skipToCharacter(rstringDelimiter, 1);
            Character nextC = parser.getCharAt(i);
            // Now check that the next character is also a delimiter to ensure that we have "".....""
            // In that case we ignore this rstringDelimiter
            if (nextC != null && (parser.getCharAt(i + 1) != null && parser.getCharAt(i + 1) == rstringDelimiter)) {
                parser.log("While parsing a string, we found a valid starting doubled quote");
                doubledQuotes = true;
                parser.setIndex(parser.getIndex() + 1);
            } else {
                // Ok this is not a doubled quote, check if this is an empty string or not
                i = parser.skipWhitespacesAt(1, false);
                nextC = parser.getCharAt(i);
                if (nextC != null && (JsonParser.STRING_DELIMITERS.contains(nextC) ||
                        Arrays.asList('{', '[').contains(nextC))) {
                    // something fishy is going on here
                    parser.log("While parsing a string, we found a doubled quote but also another quote afterwards, ignoring it");
                    parser.setIndex(parser.getIndex() + 1);
                    return "";
                } else if (nextC == null || !Arrays.asList(',', ']', '}').contains(nextC)) {
                    parser.log("While parsing a string, we found a doubled quote but it was a mistake, removing one quote");
                    parser.setIndex(parser.getIndex() + 1);
                }
            }
        }

        // Initialize our return value
        StringBuilder stringAcc = new StringBuilder();

        // Here things get a bit hairy because a string missing the final quote can also be a key or a value in an object
        // In that case we need to use the ":|,|}" characters as terminators of the string
        // So this will stop if:
        // * It finds a closing quote
        // * It iterated over the entire sequence
        // * If we are fixing missing quotes in an object, when it finds the special terminators
        ch = parser.getCharAt();
        boolean unmatchedDelimiter = false;

        while (ch != null && ch != rstringDelimiter) {
            if (missingQuotes) {
                if (parser.getContext().getCurrent() == ContextValues.OBJECT_KEY && (ch == ':' || Character.isWhitespace(ch))) {
                    parser.log("While parsing a string missing the left delimiter in object key context, we found a :, stopping here");
                    break;
                } else if (parser.getContext().getCurrent() == ContextValues.ARRAY && (ch == ']' || ch == ',')) {
                    parser.log("While parsing a string missing the left delimiter in array context, we found a ] or ,, stopping here");
                    break;
                }
            }

            if (!parser.isStreamStable() &&
                    parser.getContext().getCurrent() == ContextValues.OBJECT_VALUE &&
                    Arrays.asList(',', '}').contains(ch) &&
                    (stringAcc.length() == 0 || stringAcc.charAt(stringAcc.length() - 1) != rstringDelimiter)) {

                boolean rstringDelimiterMissing = true;
                // check if this is a case in which the closing comma is NOT missing instead
                parser.skipWhitespacesAt();
                if (parser.getCharAt(1) != null && parser.getCharAt(1) == '\\') {
                    // Ok this is a quoted string, skip
                    rstringDelimiterMissing = false;
                }
                int i = parser.skipToCharacter(rstringDelimiter, 1);
                Character nextC = parser.getCharAt(i);
                if (nextC != null) {
                    i += 1;
                    // found a delimiter, now we need to check that is followed strictly by a comma or brace
                    // or the string ended
                    i = parser.skipWhitespacesAt(i, false);
                    nextC = parser.getCharAt(i);
                    if (nextC == null || Arrays.asList(',', '}').contains(nextC)) {
                        rstringDelimiterMissing = false;
                    } else {
                        // OK but this could still be some garbage at the end of the string
                        // So we need to check if we find a new lstringDelimiter afterwards
                        // If we do, maybe this is a missing delimiter
                        i = parser.skipToCharacter(lstringDelimiter, i);
                        nextC = parser.getCharAt(i);
                        if (nextC == null) {
                            rstringDelimiterMissing = false;
                        } else {
                            // But again, this could just be something a bit stupid like "lorem, "ipsum" sic"
                            // Check if we find a : afterwards (skipping space)
                            i = parser.skipWhitespacesAt(i + 1, false);
                            nextC = parser.getCharAt(i);
                            if (nextC != null && nextC != ':') {
                                rstringDelimiterMissing = false;
                            }
                        }
                    }
                } else {
                    // There could be a case in which even the next key:value is missing delimeters
                    // because it might be a systemic issue with the output
                    // So let's check if we can find a : in the string instead
                    i = parser.skipToCharacter(':', 1);
                    nextC = parser.getCharAt(i);
                    if (nextC != null) {
                        // OK then this is a systemic issue with the output
                        break;
                    } else {
                        // skip any whitespace first
                        i = parser.skipWhitespacesAt(1, false);
                        // We couldn't find any rstringDelimeter before the end of the string
                        // check if this is the last string of an object and therefore we can keep going
                        // make an exception if this is the last char before the closing brace
                        int j = parser.skipToCharacter('}', i);
                        if (j - i > 1) {
                            // Ok it's not right after the comma
                            // Let's ignore
                            rstringDelimiterMissing = false;
                        }
                        // Check that j was not out of bound
                        else if (parser.getCharAt(j) != null) {
                            // Check for an unmatched opening brace in stringAcc
                            String stringAccStr = stringAcc.toString();
                            for (int k = stringAccStr.length() - 1; k >= 0; k--) {
                                if (stringAccStr.charAt(k) == '{') {
                                    // Ok then this is part of the string
                                    rstringDelimiterMissing = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (rstringDelimiterMissing) {
                    parser.log("While parsing a string missing the left delimiter in object value context, we found a , or } and we couldn't determine that a right delimiter was present. Stopping here");
                    break;
                }
            }

            if (!parser.isStreamStable() &&
                    ch == ']' &&
                    parser.getContext().contains(ContextValues.ARRAY) &&
                    stringAcc.length() > 0 && stringAcc.charAt(stringAcc.length() - 1) != rstringDelimiter) {
                // We found the end of an array and we are in array context
                // So let's check if we find a rstringDelimiter forward otherwise end early
                int i = parser.skipToCharacter(rstringDelimiter, 0);
                if (parser.getCharAt(i) == null) {
                    // No delimiter found
                    break;
                }
            }

            stringAcc.append(ch);
            parser.setIndex(parser.getIndex() + 1);
            ch = parser.getCharAt();

            // Unclosed string ends with a \ character. This character is ignored if streamStable = True.
            if (parser.isStreamStable() && ch == null && stringAcc.length() > 0 &&
                    stringAcc.charAt(stringAcc.length() - 1) == '\\') {
                stringAcc.setLength(stringAcc.length() - 1);
            }

            if (ch != null && stringAcc.length() > 0 && stringAcc.charAt(stringAcc.length() - 1) == '\\') {
                // This is a special case, if people use real strings this might happen
                parser.log("Found a stray escape sequence, normalizing it");
                if (Arrays.asList(rstringDelimiter, 't', 'n', 'r', 'b', '\\').contains(ch)) {
                    stringAcc.setLength(stringAcc.length() - 1);
                    Map<Character, Character> escapeSeqs = new HashMap<>();
                    escapeSeqs.put('t', '\t');
                    escapeSeqs.put('n', '\n');
                    escapeSeqs.put('r', '\r');
                    escapeSeqs.put('b', '\b');
                    stringAcc.append(escapeSeqs.getOrDefault(ch, ch));
                    parser.setIndex(parser.getIndex() + 1);
                    ch = parser.getCharAt();
                    while (ch != null && stringAcc.length() > 0 &&
                            stringAcc.charAt(stringAcc.length() - 1) == '\\' &&
                            Arrays.asList(rstringDelimiter, '\\').contains(ch)) {
                        // this is a bit of a special case, if I don't do this it will close the loop or create a train of \\
                        // I don't love it though
                        stringAcc.setLength(stringAcc.length() - 1);
                        stringAcc.append(ch);
                        parser.setIndex(parser.getIndex() + 1);
                        ch = parser.getCharAt();
                    }
                    continue;
                } else if (Arrays.asList('u', 'x').contains(ch)) {
                    // If we find a unicode escape sequence, normalize it
                    int numChars = ch == 'u' ? 4 : 2;
                    String nextChars = parser.getJsonStr().substring(
                            Math.min(parser.getIndex() + 1, parser.getJsonStr().length()),
                            Math.min(parser.getIndex() + 1 + numChars, parser.getJsonStr().length())
                    );
                    if (nextChars.length() == numChars && nextChars.matches("[0-9a-fA-F]+")) {
                        parser.log("Found a unicode escape sequence, normalizing it");
                        stringAcc.setLength(stringAcc.length() - 1);
                        stringAcc.append((char) Integer.parseInt(nextChars, 16));
                        parser.setIndex(parser.getIndex() + 1 + numChars);
                        ch = parser.getCharAt();
                        continue;
                    }
                } else if (JsonParser.STRING_DELIMITERS.contains(ch) && ch != rstringDelimiter) {
                    parser.log("Found a delimiter that was escaped but shouldn't be escaped, removing the escape");
                    stringAcc.setLength(stringAcc.length() - 1);
                    stringAcc.append(ch);
                    parser.setIndex(parser.getIndex() + 1);
                    ch = parser.getCharAt();
                    continue;
                }
            }

            // If we are in object key context and we find a colon, it could be a missing right quote
            if (ch != null && ch == ':' && !missingQuotes &&
                    parser.getContext().getCurrent() == ContextValues.OBJECT_KEY) {
                // Ok now we need to check if this is followed by a value like "..."
                int i = parser.skipToCharacter(lstringDelimiter, 1);
                Character nextC = parser.getCharAt(i);
                if (nextC != null) {
                    i += 1;
                    // found the first delimiter
                    i = parser.skipToCharacter(rstringDelimiter, i);
                    nextC = parser.getCharAt(i);
                    if (nextC != null) {
                        // found a second delimiter
                        i += 1;
                        // Skip spaces
                        i = parser.skipWhitespacesAt(i, false);
                        nextC = parser.getCharAt(i);
                        if (nextC != null && Arrays.asList(',', '}').contains(nextC)) {
                            // Ok then this is a missing right quote
                            parser.log("While parsing a string missing the right delimiter in object key context, we found a :, stopping here");
                            break;
                        }
                    }
                } else {
                    // The string ended without finding a lstringDelimiter, I will assume this is a missing right quote
                    parser.log("While parsing a string missing the right delimiter in object key context, we found a :, stopping here");
                    break;
                }
            }

            // ChatGPT sometimes forget to quote stuff in html tags or markdown, so we do this whole thing here
            if (ch != null && ch == rstringDelimiter &&
                    (stringAcc.length() == 0 || stringAcc.charAt(stringAcc.length() - 1) != '\\')) {

                // Special case here, in case of double quotes one after another
                if (doubledQuotes && parser.getCharAt(1) != null && parser.getCharAt(1) == rstringDelimiter) {
                    parser.log("While parsing a string, we found a doubled quote, ignoring it");
                    parser.setIndex(parser.getIndex() + 1);
                } else if (missingQuotes && parser.getContext().getCurrent() == ContextValues.OBJECT_VALUE) {
                    // In case of missing starting quote I need to check if the delimeter is the end or the beginning of a key
                    int i = 1;
                    Character nextC = parser.getCharAt(i);
                    while (nextC != null && !Arrays.asList(rstringDelimiter, lstringDelimiter).contains(nextC)) {
                        i++;
                        nextC = parser.getCharAt(i);
                    }
                    if (nextC != null) {
                        // We found a quote, now let's make sure there's a ":" following
                        i += 1;
                        // found a delimiter, now we need to check that is followed strictly by a comma or brace
                        i = parser.skipWhitespacesAt(i, false);
                        nextC = parser.getCharAt(i);
                        if (nextC != null && nextC == ':') {
                            // Reset the cursor
                            parser.setIndex(parser.getIndex() - 1);
                            ch = parser.getCharAt();
                            parser.log("In a string with missing quotes and object value context, I found a delimeter but it turns out it was the beginning on the next key. Stopping here.");
                            break;
                        }
                    }
                } else if (unmatchedDelimiter) {
                    unmatchedDelimiter = false;
                    stringAcc.append(ch);
                    parser.setIndex(parser.getIndex() + 1);
                    ch = parser.getCharAt();
                } else {
                    // Check if eventually there is a rstringDelimiter, otherwise we bail
                    int i = 1;
                    Character nextC = parser.getCharAt(i);
                    boolean checkCommaInObjectValue = true;
                    while (nextC != null && !Arrays.asList(rstringDelimiter, lstringDelimiter).contains(nextC)) {
                        // This is a bit of a weird workaround, essentially in object_value context we don't always break on commas
                        // This is because the routine after will make sure to correct any bad guess and this solves a corner case
                        if (checkCommaInObjectValue && Character.isLetter(nextC)) {
                            checkCommaInObjectValue = false;
                        }
                        // If we are in an object context, let's check for the right delimiters
                        if ((parser.getContext().contains(ContextValues.OBJECT_KEY) && Arrays.asList(':', '}').contains(nextC)) ||
                                (parser.getContext().contains(ContextValues.OBJECT_VALUE) && nextC == '}') ||
                                (parser.getContext().contains(ContextValues.ARRAY) && Arrays.asList(']', ',').contains(nextC)) ||
                                (checkCommaInObjectValue && parser.getContext().getCurrent() == ContextValues.OBJECT_VALUE && nextC == ',')) {
                            break;
                        }
                        i++;
                        nextC = parser.getCharAt(i);
                    }
                    // If we stopped for a comma in object_value context, let's check if find a "} at the end of the string
                    if (nextC != null && nextC == ',' && parser.getContext().getCurrent() == ContextValues.OBJECT_VALUE) {
                        i += 1;
                        i = parser.skipToCharacter(rstringDelimiter, i);
                        nextC = parser.getCharAt(i);
                        // Ok now I found a delimiter, let's skip whitespaces and see if next we find a } or a ,
                        i += 1;
                        i = parser.skipWhitespacesAt(i, false);
                        nextC = parser.getCharAt(i);
                        if (nextC != null && Arrays.asList('}', ',').contains(nextC)) {
                            parser.log("While parsing a string, we a misplaced quote that would have closed the string but has a different meaning here, ignoring it");
                            stringAcc.append(ch);
                            parser.setIndex(parser.getIndex() + 1);
                            ch = parser.getCharAt();
                            continue;
                        }
                    } else if (nextC != null && nextC == rstringDelimiter &&
                            (i == 1 || parser.getCharAt(i - 1) != '\\')) {
                        // Check if self.index:self.index+i is only whitespaces, break if that's the case
                        boolean allWhitespace = true;
                        for (int j = 1; j < i; j++) {
                            Character cAtJ = parser.getCharAt(j);
                            if (cAtJ != null && !Character.isWhitespace(cAtJ)) {
                                allWhitespace = false;
                                break;
                            }
                        }
                        if (allWhitespace) {
                            break;
                        }
                        if (parser.getContext().getCurrent() == ContextValues.OBJECT_VALUE) {
                            i = parser.skipWhitespacesAt(i + 1, false);
                            if (parser.getCharAt(i) != null && parser.getCharAt(i) == ',') {
                                // So we found a comma, this could be a case of a single quote like "va"lue",
                                // Search if it's followed by another key, starting with the first delimeter
                                i = parser.skipToCharacter(lstringDelimiter, i + 1);
                                i += 1;
                                i = parser.skipToCharacter(rstringDelimiter, i + 1);
                                i += 1;
                                i = parser.skipWhitespacesAt(i, false);
                                nextC = parser.getCharAt(i);
                                if (nextC != null && nextC == ':') {
                                    parser.log("While parsing a string, we a misplaced quote that would have closed the string but has a different meaning here, ignoring it");
                                    stringAcc.append(ch);
                                    parser.setIndex(parser.getIndex() + 1);
                                    ch = parser.getCharAt();
                                    continue;
                                }
                            }
                            // We found a delimiter and we need to check if this is a key
                            // so find a rstringDelimiter and a colon after
                            i = parser.skipToCharacter(rstringDelimiter, i + 1);
                            i += 1;
                            nextC = parser.getCharAt(i);
                            while (nextC != null && nextC != ':') {
                                if (Arrays.asList(',', ']', '}').contains(nextC) ||
                                        (nextC == rstringDelimiter && (i == 0 || parser.getCharAt(i - 1) != '\\'))) {
                                    break;
                                }
                                i++;
                                nextC = parser.getCharAt(i);
                            }
                            // Only if we fail to find a ':' then we know this is misplaced quote
                            if (nextC == null || nextC != ':') {
                                parser.log("While parsing a string, we a misplaced quote that would have closed the string but has a different meaning here, ignoring it");
                                unmatchedDelimiter = !unmatchedDelimiter;
                                stringAcc.append(ch);
                                parser.setIndex(parser.getIndex() + 1);
                                ch = parser.getCharAt();
                            }
                        } else if (parser.getContext().getCurrent() == ContextValues.ARRAY) {
                            // Let's check if after this quote there are two quotes in a row followed by a comma or a closing bracket
                            i = parser.skipToCharacter(Arrays.asList(rstringDelimiter, ']'), i + 1);
                            nextC = parser.getCharAt(i);
                            boolean evenDelimiters = nextC != null && nextC == rstringDelimiter;
                            while (evenDelimiters && nextC != null && nextC == rstringDelimiter) {
                                i = parser.skipToCharacter(Arrays.asList(rstringDelimiter, ']'), i + 1);
                                i = parser.skipToCharacter(Arrays.asList(rstringDelimiter, ']'), i + 1);
                                nextC = parser.getCharAt(i);
                            }
                            if (evenDelimiters && (nextC == null || nextC != ']')) {
                                // If we got up to here it means that this is a situation like this:
                                // ["bla bla bla "puppy" bla bla bla "kitty" bla bla"]
                                // So we need to ignore this quote
                                parser.log("While parsing a string in Array context, we detected a quoted section that would have closed the string but has a different meaning here, ignoring it");
                                unmatchedDelimiter = !unmatchedDelimiter;
                                stringAcc.append(ch);
                                parser.setIndex(parser.getIndex() + 1);
                                ch = parser.getCharAt();
                            } else {
                                break;
                            }
                        } else if (parser.getContext().getCurrent() == ContextValues.OBJECT_KEY) {
                            // In this case we just ignore this and move on
                            parser.log("While parsing a string in Object Key context, we detected a quoted section that would have closed the string but has a different meaning here, ignoring it");
                            stringAcc.append(ch);
                            parser.setIndex(parser.getIndex() + 1);
                            ch = parser.getCharAt();
                        }
                    }
                }
            }
        }

        if (ch != null && missingQuotes && parser.getContext().getCurrent() == ContextValues.OBJECT_KEY &&
                Character.isWhitespace(ch)) {
            parser.log("While parsing a string, handling an extreme corner case in which the LLM added a comment instead of valid string, invalidate the string and return an empty value");
            parser.skipWhitespacesAt();
            if (parser.getCharAt() == null || (parser.getCharAt() != ':' && parser.getCharAt() != ',')) {
                return "";
            }
        }

        // A fallout of the previous special case in the while loop,
        // we need to update the index only if we had a closing quote
        if (ch == null || ch != rstringDelimiter) {
            // if streamStable = True, unclosed strings do not trim trailing whitespace characters
            if (!parser.isStreamStable()) {
                parser.log("While parsing a string, we missed the closing quote, ignoring");
                // Trim trailing whitespace
                while (stringAcc.length() > 0 && Character.isWhitespace(stringAcc.charAt(stringAcc.length() - 1))) {
                    stringAcc.setLength(stringAcc.length() - 1);
                }
            }
        } else {
            parser.setIndex(parser.getIndex() + 1);
        }

        if (!parser.isStreamStable() && (missingQuotes || (stringAcc.length() > 0 && stringAcc.charAt(stringAcc.length() - 1) == '\n'))) {
            // Clean the whitespaces for some corner cases
            while (stringAcc.length() > 0 && Character.isWhitespace(stringAcc.charAt(stringAcc.length() - 1))) {
                stringAcc.setLength(stringAcc.length() - 1);
            }
        }

        return stringAcc.toString();
    }
}
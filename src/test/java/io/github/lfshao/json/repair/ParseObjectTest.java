package io.github.lfshao.json.repair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 基于Python test_parse_object.py的Java测试类
 */
public class ParseObjectTest {

    @Test
    public void testParseObject() {
        assertEquals("{}", JsonRepair.repair("{}"));
        assertEquals("{\"key\":\"value\",\"key2\":1,\"key3\":true}",
                JsonRepair.repair("{ \"key\": \"value\", \"key2\": 1, \"key3\": True }"));
        assertEquals("{}", JsonRepair.repair("{"));
        assertEquals("{\"key\":\"value\",\"key2\":1,\"key3\":null}",
                JsonRepair.repair("{ \"key\": value, \"key2\": 1 \"key3\": null }"));
        assertEquals("{}", JsonRepair.repair("   {  }   "));
        assertEquals("{}", JsonRepair.repair("{"));
        assertEquals("", JsonRepair.repair("}"));
        assertEquals("{}", JsonRepair.repair("{\""));
    }

    @Test
    public void testParseObjectEdgeCases() {
        assertEquals("{\"foo\":[]}", JsonRepair.repair("{foo: [}"));
        assertEquals("{}", JsonRepair.repair("{       "));
        assertEquals("{}", JsonRepair.repair("{\"\"): \"value\""));
        assertEquals("{\"value_1\":true,\"value_2\":\"data\"}",
                JsonRepair.repair("{\"value_1\": true, COMMENT \"value_2\": \"data\"}"));
        assertEquals("{\"value_1\":true,\"value_2\":\"data\"}",
                JsonRepair.repair("{\"value_1\": true, SHOULD_NOT_EXIST \"value_2\": \"data\" AAAA }"));
        assertEquals("{\"\":true,\"key2\":\"value2\"}",
                JsonRepair.repair("{\"\" : true, \"key2\": \"value2\"}"));
    }

    @Test
    public void testComplexObjectCases() {
        assertEquals("{\"answer\":[{\"traits\":\"Female aged 60+\",\"answer1\":\"5\"}]}",
                JsonRepair.repair("{\"\"answer\"\":[{\"\"traits\"\":''Female aged 60+'',\"\"answer1\"\":\"\"5\"\"}]}"));

        assertEquals("{\"words\":\"abcdef\",\"numbers\":12345,\"words2\":\"ghijkl\"}",
                JsonRepair.repair("{ \"words\": abcdef\", \"numbers\": 12345\", \"words2\": ghijkl\" }"));

        assertEquals("{\"number\":1,\"reason\":\"According...\",\"ans\":\"YES\"}",
                JsonRepair.repair("{\"number\": 1,\"reason\": \"According...\"\"ans\": \"YES\"}"));

        assertEquals("{\"a\":\"{ b\"}",
                JsonRepair.repair("{ \"a\" : \"{ b\": {} }\" }"));

        assertEquals("{\"b\":\"xxxxx\"}",
                JsonRepair.repair("{\"b\": \"xxxxx\" true}"));

        assertEquals("{\"key\":\"Lorem \\\"ipsum\\\" s,\"}",
                JsonRepair.repair("{\"key\": \"Lorem \\\"ipsum\\\" s,\"}"));

        assertEquals("{\"lorem\":\"ipsum, sic, datum.\"}",
                JsonRepair.repair("{\"lorem\": ipsum, sic, datum.\",}"));
    }

    @Test
    public void testMissingColonsAndCommas() {
        assertEquals("{\"key\":\"value\"}", JsonRepair.repair("{\"key\" \"value\"}"));
        assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}", JsonRepair.repair("{\"key\": \"value\" \"key2\": \"value2\"}"));
        assertEquals("{\"key\":\"value\",\"key2\":23}", JsonRepair.repair("{\"key\": \"value\", \"key2\" 123}"));
    }

    @Test
    public void testIncompleteObjects() {
        assertEquals("{\"key\":\"value\"}", JsonRepair.repair("{\"key\": \"value\""));
        assertEquals("{\"key\":\"value\",\"key2\":123}", JsonRepair.repair("{\"key\": \"value\", \"key2\": 123"));
        assertEquals("{\"nested\":{\"key\":\"value\"}}", JsonRepair.repair("{\"nested\": {\"key\": \"value\""));
    }

    @Test
    public void testObjectWithArrays() {
        assertEquals("{\"items\":[1,2,3]}", JsonRepair.repair("{\"items\": [1, 2, 3]}"));
        assertEquals("{\"items\":[1,2,3]}", JsonRepair.repair("{\"items\": [1, 2, 3"));
        assertEquals("{\"nested\":{\"items\":[\"a\",\"b\"]}}", JsonRepair.repair("{\"nested\": {\"items\": [\"a\", \"b\"]}}"));
    }

    @Test
    public void testObjectWithSpecialCharacters() {
        assertEquals("{\"key with spaces\":\"value\"}", JsonRepair.repair("{\"key with spaces\": \"value\"}"));
        assertEquals("{\"key-with-dashes\":\"value\"}", JsonRepair.repair("{\"key-with-dashes\": \"value\"}"));
        assertEquals("{\"key_with_underscores\":\"value\"}", JsonRepair.repair("{\"key_with_underscores\": \"value\"}"));
    }
} 
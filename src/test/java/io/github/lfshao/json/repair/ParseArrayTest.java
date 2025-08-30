package io.github.lfshao.json.repair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 基于Python test_parse_array.py的Java测试类
 */
public class ParseArrayTest {

    @Test
    public void testParseArray() {
        assertEquals("[]", JsonRepair.repair("[]"));
        assertEquals("[1,2,3,4]", JsonRepair.repair("[1, 2, 3, 4]"));
        assertEquals("[]", JsonRepair.repair("["));
        assertEquals("[[1]]", JsonRepair.repair("[[1\n\n]"));
    }

    @Test
    public void testParseArrayEdgeCases() {
        assertEquals("[]", JsonRepair.repair("[{]"));
        assertEquals("[]", JsonRepair.repair("["));
        assertEquals("[]", JsonRepair.repair("[\""));
        assertEquals("", JsonRepair.repair("]"));
        assertEquals("[1,2,3]", JsonRepair.repair("[1, 2, 3,"));
        assertEquals("[1,2,3]", JsonRepair.repair("[1, 2, 3, ...]"));
        assertEquals("[1,2,3]", JsonRepair.repair("[1, 2, ... , 3]"));
        assertEquals("[1,2,\"...\",3]", JsonRepair.repair("[1, 2, '...', 3]"));
        assertEquals("[true,false,null]", JsonRepair.repair("[true, false, null, ...]"));
        assertEquals("[\"a\",\"b\",\"c\",1]", JsonRepair.repair("[\"a\" \"b\" \"c\" 1"));
        assertEquals("{\"employees\":[\"John\",\"Anna\"]}", JsonRepair.repair("{\"employees\":[\"John\", \"Anna\","));
        assertEquals("{\"employees\":[\"John\",\"Anna\",\"Peter\"]}", JsonRepair.repair("{\"employees\":[\"John\", \"Anna\", \"Peter\""));
        assertEquals("{\"key1\":{\"key2\":[1,2,3]}}", JsonRepair.repair("{\"key1\": {\"key2\": [1, 2, 3"));
        assertEquals("{\"key\":[\"value\"]}", JsonRepair.repair("{\"key\": [\"value\"]}"));
    }

    @Test
    public void testArrayWithQuotedStrings() {
        assertEquals("[\"lorem \\\"ipsum\\\" sic\"]", JsonRepair.repair("[\"lorem \\\"ipsum\\\" sic\"]"));

        assertEquals("{\"key1\":[\"value1\",\"value2\"],\"key2\":[\"value3\",\"value4\"]}",
                JsonRepair.repair("{\"key1\": [\"value1\", \"value2\"], \"key2\": [\"value3\", \"value4\"]}"));

        assertEquals("{\"key\":[\"value\",\"value1\",\"value2\"]}",
                JsonRepair.repair("{\"key\": [\"value\" \"value1\" \"value2\"]}"));
    }

    @Test
    public void testComplexArrayCases() {
        assertEquals("{\"k\\\"e\\\"y\":\"value\"}",
                JsonRepair.repair("{\"k\"e\"y\": \"value\"}"));

        assertEquals("[{\"key\":\"value\"}]",
                JsonRepair.repair("[\"key\":\"value\"}]"));

        assertEquals("[{\"key\":\"value\"}]",
                JsonRepair.repair("[{\"key\": \"value\", \"key\""));

        assertEquals("[\"value1\",\"value2\",\"value3\"]",
                JsonRepair.repair("[\"value1\" value2\", \"value3\"]"));
    }

    @Test
    public void testArrayWithComments() {
        assertEquals("{\"bad_one\":[\"Lorem Ipsum\",\"consectetur\",\"comment\"],\"good_one\":[\"elit\",\"sed\",\"tempor\"]}",
                JsonRepair.repair("{\"bad_one\":[\"Lorem Ipsum\", \"consectetur\" comment\" ], \"good_one\":[ \"elit\", \"sed\", \"tempor\"]}"));

        assertEquals("{\"bad_one\":[\"Lorem Ipsum\",\"consectetur\",\"comment\"],\"good_one\":[\"elit\",\"sed\",\"tempor\"]}",
                JsonRepair.repair("{\"bad_one\": [\"Lorem Ipsum\",\"consectetur\" comment],\"good_one\": [\"elit\",\"sed\",\"tempor\"]}"));
    }

    @Test
    public void testIncompleteArrays() {
        assertEquals("[1,2,3]", JsonRepair.repair("[1, 2, 3"));
        assertEquals("[\"item1\",\"item2\"]", JsonRepair.repair("[\"item1\", \"item2\""));
        assertEquals("[{\"key\":\"value\"}]", JsonRepair.repair("[{\"key\": \"value\""));
    }

    @Test
    public void testNestedArrays() {
        assertEquals("[[1,2],[3,4]]", JsonRepair.repair("[[1, 2], [3, 4]]"));
        assertEquals("[[1,2],[3,4]]", JsonRepair.repair("[[1, 2], [3, 4"));
        assertEquals("[{\"arr\":[1,2,3]}]", JsonRepair.repair("[{\"arr\": [1, 2, 3]}]"));
    }
} 
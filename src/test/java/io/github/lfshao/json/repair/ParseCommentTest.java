package io.github.lfshao.json.repair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基于Python test_parse_comment.py的Java测试类
 */
public class ParseCommentTest {

    @Test
    public void testParseComment() {
        assertEquals("", JsonRepair.repair("/"));

        // 注意：Python版本中这个测试没有断言，我们添加一个基本的断言
        String result = JsonRepair.repair("/* comment */ {\"key\": \"value\"}");
        assertNotNull(result);
        assertTrue(result.contains("key") || result.contains("value"));

        assertEquals("{\"key\":{\"key2\":\"value2\"},\"key3\":\"value3\"}",
                JsonRepair.repair("{ \"key\": { \"key2\": \"value2\" // comment }, \"key3\": \"value3\" }"));

        assertEquals("{\"key\":{\"key2\":\"value2\"},\"key3\":\"value3\"}",
                JsonRepair.repair("{ \"key\": { \"key2\": \"value2\" # comment }, \"key3\": \"value3\" }"));

        assertEquals("{\"key\":{\"key2\":\"value2\"},\"key3\":\"value3\"}",
                JsonRepair.repair("{ \"key\": { \"key2\": \"value2\" /* comment */ }, \"key3\": \"value3\" }"));

        assertEquals("[\"value\",\"value2\"]",
                JsonRepair.repair("[ \"value\", /* comment */ \"value2\" ]"));

        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{ \"key\": \"value\" /* comment"));
    }

    @Test
    public void testLineComments() {
        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key\": \"value\" // this is a comment}"));

        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key\": \"value\" # this is also a comment}"));

        assertEquals("[1,2,3]",
                JsonRepair.repair("[1, 2, 3 // comment at end]"));
    }

    @Test
    public void testBlockComments() {
        // 根据Python版本的实际行为：在值位置的注释导致值为空字符串
        assertEquals("{\"key\":\"\"}",
                JsonRepair.repair("{\"key\": /* comment */ \"value\"}"));

        // 在键值对之间的注释被正确忽略
        assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}",
                JsonRepair.repair("{\"key\": \"value\", /* multi\nline\ncomment */ \"key2\": \"value2\"}"));

        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key\": \"value\" /* unclosed comment"));
    }

    @Test
    public void testCommentsInArrays() {
        assertEquals("[\"item1\",\"item2\"]",
                JsonRepair.repair("[\"item1\", // comment\n\"item2\"]"));

        assertEquals("[\"item1\",\"item2\"]",
                JsonRepair.repair("[\"item1\", /* comment */ \"item2\"]"));

        assertEquals("[1,2,3]",
                JsonRepair.repair("[1, /* comment */ 2, 3]"));
    }

    @Test
    public void testCommentsInObjects() {
        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}",
                JsonRepair.repair("{\"key1\": \"value1\", // comment\n\"key2\": \"value2\"}"));

        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}",
                JsonRepair.repair("{\"key1\": \"value1\", /* comment */ \"key2\": \"value2\"}"));
    }

    @Test
    public void testMixedComments() {
        // 根据Python版本的实际行为：在值位置的注释导致值为空字符串
        assertEquals("{\"key\":\"\"}",
                JsonRepair.repair("{ // line comment\n\"key\": /* block comment */ \"value\" }"));

        assertEquals("[1,2,3]",
                JsonRepair.repair("[ // start comment\n1, /* middle */ 2, 3 // end comment\n]"));
    }

    @Test
    public void testCommentEdgeCases() {
        assertEquals("", JsonRepair.repair("// just a comment"));
        assertEquals("", JsonRepair.repair("/* just a block comment */"));
        assertEquals("", JsonRepair.repair("# just a hash comment"));

        // 测试注释中包含JSON语法的情况
        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key\": \"value\" /* this comment has { and } */ }"));
    }
} 
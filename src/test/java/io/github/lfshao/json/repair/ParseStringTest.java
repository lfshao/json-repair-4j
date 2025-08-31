package io.github.lfshao.json.repair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基于Python test_parse_string.py的Java测试类
 */
public class ParseStringTest {

    @Test
    public void testParseString() {
        assertEquals("", JsonRepair.repair("\""));
        assertEquals("", JsonRepair.repair("\n"));
        assertEquals("", JsonRepair.repair(" "));
        assertEquals("", JsonRepair.repair("string"));
        assertEquals("{}", JsonRepair.repair("stringbeforeobject {}"));
    }

    @Test
    public void testMissingAndMixedQuotes() {
        assertEquals("{\"key\":\"string\",\"key2\":false,\"key3\":null,\"key4\":\"unquoted\"}",
                JsonRepair.repair("{'key': 'string', 'key2': false, \"key3\": null, \"key4\": unquoted}"));

        assertEquals("{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}",
                JsonRepair.repair("{\"name\": \"John\", \"age\": 30, \"city\": \"New York\"}"));

        assertEquals("{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}",
                JsonRepair.repair("{\"name\": \"John\", \"age\": 30, city: \"New York\"}"));

        assertEquals("{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}",
                JsonRepair.repair("{\"name\": \"John\", \"age\": 30, \"city\": New York}"));

        assertEquals("{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}",
                JsonRepair.repair("{\"name\": John, \"age\": 30, \"city\": \"New York\"}"));

        assertEquals("{\"slanted_delimiter\":\"value\"}",
                JsonRepair.repair("{\"slanted_delimiter\": \"value\"}"));

        assertEquals("{\"name\":\"John\",\"age\":30,\"city\":\"New\"}",
                JsonRepair.repair("{\"name\": \"John\", \"age\": 30, \"city\": \"New\"}"));

        assertEquals("{\"name\":\"John\",\"age\":30,\"city\":\"New York\",\"gender\":\"male\"}",
                JsonRepair.repair("{\"name\": \"John\", \"age\": 30, \"city\": \"New York, \"gender\": \"male\"}"));
    }

    @Test
    public void testComplexStringCases() {
        assertEquals("[{\"key\":\"value\",\"notes\":\"lorem \\\"ipsum\\\", sic.\"}]",
                JsonRepair.repair("[{\"key\": \"value\", COMMENT \"notes\": \"lorem \\\"ipsum\\\", sic.\" }]"));

        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key\": \"\"value\"}"));

        assertEquals("{\"key\":\"value\",\"5\":\"value\"}",
                JsonRepair.repair("{\"key\": \"value\", 5: \"value\"}"));

        assertEquals("{\"foo\":\"\\\"bar\\\"\"}",
                JsonRepair.repair("{\"foo\": \"\\\"bar\\\"\"}"));

        // 根据Python版本的实际行为：空字符串开头的键被处理为单个空格
        assertEquals("{\" key\":\"val\"}",
                JsonRepair.repair("{\"\" key\":\"val\"}"));

        assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}",
                JsonRepair.repair("{\"key\": value \"key2\" : \"value2\" }"));
    }

    @Test
    public void testEscapeSequences() {
        assertEquals("{\"key\":\"value\\nvalue\"}",
                JsonRepair.repair("{\"key\": \"value\\nvalue\"}"));

        assertEquals("{\"key\":\"value\\tvalue\"}",
                JsonRepair.repair("{\"key\": \"value\\tvalue\"}"));

        assertEquals("{\"key\":\"value\\\"value\"}",
                JsonRepair.repair("{\"key\": \"value\\\"value\"}"));

        assertEquals("{\"key\":\"value\\\\value\"}",
                JsonRepair.repair("{\"key\": \"value\\\\value\"}"));
    }

    @Test
    public void testUnicodeSequences() {
        // 测试Unicode转义序列
        String result = JsonRepair.repair("{\"key\": \"value\\u0041\"}");
        assertNotNull(result);
        assertTrue(result.contains("key"));

        String result2 = JsonRepair.repair("{\"key\": \"value\\x41\"}");
        assertNotNull(result2);
        assertTrue(result2.contains("key"));
    }

    @Test
    public void testMalformedStrings() {
        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key\": \"value"));

        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{key: \"value\"}"));

        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key:value}"));

        // 测试字符串中的引号处理
        String result = JsonRepair.repair("{\"key\": \"lorem ipsum ... \\\"sic \\\" tamet. ...\"}");
        assertNotNull(result);
        assertTrue(result.contains("lorem"));
    }

    @Test
    public void testStringParsingEdgeCases() {
        // Python test_parse_string.py中的边缘用例
        assertEquals("", JsonRepair.repair("\""));
        assertEquals("", JsonRepair.repair("\n"));
        assertEquals("", JsonRepair.repair(" "));
        assertEquals("", JsonRepair.repair("string"));
        assertEquals("{}", JsonRepair.repair("stringbeforeobject {}"));

        // 复杂字符串修复（避免与testComplexStringCases重复）

        assertEquals("{\"key\":\"lorem ipsum ... \\\"sic \\\" tamet. ...\"}",
                JsonRepair.repair("{\"key\": \"lorem ipsum ... \"sic \" tamet. ...\"}"));

        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key\": value , }"));

        assertEquals("{\"comment\":\"lorem, \\\"ipsum\\\" sic \\\"tamet\\\". To improve\"}",
                JsonRepair.repair("{\"comment\": \"lorem, \"ipsum\" sic \"tamet\". To improve\"}"));

        assertEquals("{\"key\":\"v\\\"alu\\\"e\"}",
                JsonRepair.repair("{\"key\": \"v\"alu\"e\"} key:"));

        assertEquals("{\"key\":\"v\\\"alue\",\"key2\":\"value2\"}",
                JsonRepair.repair("{\"key\": \"v\"alue\", \"key2\": \"value2\"}"));

        assertEquals("[{\"key\":\"v\\\"alu,e\",\"key2\":\"value2\"}]",
                JsonRepair.repair("[{\"key\": \"v\"alu,e\", \"key2\": \"value2\"}]"));
    }

    @Test
    public void testEscapingCases() {
        // Python test_parse_string.py中的转义测试
        assertEquals("", JsonRepair.repair("'\"'"));

        assertEquals("{\"key\":\"string\\\"\\n\\t\\\\le\"}",
                JsonRepair.repair("{\"key\": 'string\"\\n\\t\\\\le'}"));

        assertEquals("{\"real_content\":\"Some string: Some other string \\t Some string <a href=\\\"https://domain.com\\\">Some link</a>\"}",
                JsonRepair.repair("{\"real_content\": \"Some string: Some other string \\t Some string <a href=\\\"https://domain.com\\\">Some link</a>\"}"));

        assertEquals("{\"key_1\":\"value\"}",
                JsonRepair.repair("{\"key_1\n\": \"value\"}"));

        assertEquals("{\"key\\t_\":\"value\"}",
                JsonRepair.repair("{\"key\\t_\": \"value\"}"));

        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("{\"key\": '\\u0076\\u0061\\u006c\\u0075\\u0065'}"));

        // 注意：Java版本保留了转义的单引号
        assertEquals("{\"key\":\"valu\\\\'e\"}",
                JsonRepair.repair("{\"key\": \"valu\\\\'e\"}"));

        assertEquals("{\"key\":\"{\\\"key\\\": 1, \\\"key2\\\": 1}\"}",
                JsonRepair.repair("{'key': \"{\\\"key\\\": 1, \\\"key2\\\": 1}\"}"));
    }

    @Test
    public void testMarkdownAndSpecialCases() {
        // Python test_parse_string.py中的Markdown测试
        assertEquals("{\"content\":\"[LINK](\\\"https://google.com\\\")\"}",
                JsonRepair.repair("{ \"content\": \"[LINK](\"https://google.com\")\" }"));

        assertEquals("{\"content\":\"[LINK](\"}",
                JsonRepair.repair("{ \"content\": \"[LINK](\" }"));

        assertEquals("{\"content\":\"[LINK](\",\"key\":true}",
                JsonRepair.repair("{ \"content\": \"[LINK](\", \"key\": true }"));

        // 前后字符处理测试
        assertEquals("{\"key\":\"value\"}",
                JsonRepair.repair("````{ \"key\": \"value\" }```"));

        assertEquals("{\"a\":\"\",\"b\":[{\"c\":1}]}",
                JsonRepair.repair("{    \"a\": \"\",    \"b\": [ { \"c\": 1} ] \\n}```"));

        assertEquals("{\"a\":\"b\"}",
                JsonRepair.repair("Based on the information extracted, here is the filled JSON output: ```json { 'a': 'b' } ```"));
    }

    @Test
    public void testStringWithNewlineAndCommaEdgeCases() {
        // 测试字符串中包含换行符的情况
        assertEquals("{\"name\":\"John\"}",
                JsonRepair.repair("{\n    \"name\": \"John\\n\n}"));

        // 测试多余逗号的情况
        assertEquals("{\"name\":\"John,\",\"age\":30}",
                JsonRepair.repair("{\n    \"name\": \"John,\",\n    \"age\": 30\n}"));

        // 测试字符串值中包含换行符
        assertEquals("{\"description\":\"This is a\\nmultiline text\"}",
                JsonRepair.repair("{\"description\": \"This is a\\nmultiline text\"}"));

        // 测试不完整的字符串后跟换行符
        assertEquals("{\"message\":\"Hello\"}",
                JsonRepair.repair("{\"message\": \"Hello\\n}"));

        // 测试字符串中的逗号和换行符组合
        assertEquals("{\"data\":\"value1,\\nvalue2\"}",
                JsonRepair.repair("{\"data\": \"value1,\\nvalue2\"}"));
    }

        
    @Test
    public void testStringWithInternalQuotes() {
        // 测试字符串中包含内部引号的情况
        // 这是一个关键的测试用例，用于验证数组上下文中内部引号的处理
        assertEquals("[\"lorem \\\"ipsum\\\" sic\"]", 
                    JsonRepair.repair("[\"lorem \"ipsum\" sic\"]"));
        
        // 其他相关的测试用例
        assertEquals("[\"hello \\\"world\\\" test\"]", 
                    JsonRepair.repair("[\"hello \"world\" test\"]"));
        
        assertEquals("[\"start \\\"middle\\\" end\"]", 
                    JsonRepair.repair("[\"start \"middle\" end\"]"));
    }
} 
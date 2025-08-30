package io.github.lfshao.json.repair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 基于Python test_parse_boolean_or_null.py的Java测试类
 */
public class ParseBooleanOrNullTest {

    @Test
    public void testParseBooleanOrNull() {
        // 注意：Java版本可能与Python版本在处理单独的布尔值时有所不同
        // Python版本中单独的"True"/"False"/"Null"返回空字符串，因为它们不是有效的JSON
        assertEquals("", JsonRepair.repair("True"));
        assertEquals("", JsonRepair.repair("False"));
        assertEquals("", JsonRepair.repair("Null"));

        // 但是小写的布尔值应该被正确解析
        assertEquals("true", JsonRepair.repair("true"));
        assertEquals("false", JsonRepair.repair("false"));
        assertEquals("null", JsonRepair.repair("null"));

        // 在对象中的布尔值和null值
        assertEquals("{\"key\":true,\"key2\":false,\"key3\":null}",
                JsonRepair.repair("  {\"key\": true, \"key2\": false, \"key3\": null}"));

        // 大写的布尔值在对象中应该被转换为小写
        assertEquals("{\"key\":true,\"key2\":false,\"key3\":null}",
                JsonRepair.repair("{\"key\": TRUE, \"key2\": FALSE, \"key3\": Null}   "));
    }

    @Test
    public void testBooleanInArray() {
        assertEquals("[true,false,null]", JsonRepair.repair("[true, false, null]"));
        assertEquals("[true,false,null]", JsonRepair.repair("[TRUE, FALSE, NULL]"));
        assertEquals("[true,false,null]", JsonRepair.repair("[True, False, Null]"));
    }

    @Test
    public void testBooleanInNestedStructures() {
        assertEquals("{\"outer\":{\"inner\":true}}", JsonRepair.repair("{\"outer\": {\"inner\": true}}"));
        assertEquals("{\"array\":[true,false,null]}", JsonRepair.repair("{\"array\": [true, false, null]}"));
        assertEquals("[{\"flag\":true},{\"flag\":false}]", JsonRepair.repair("[{\"flag\": true}, {\"flag\": false}]"));
    }

    @Test
    public void testMixedCaseBooleans() {
        assertEquals("{\"a\":true,\"b\":false,\"c\":null}", JsonRepair.repair("{\"a\": tRuE, \"b\": FaLsE, \"c\": NuLl}"));
    }

    @Test
    public void testBooleanWithoutQuotes() {
        assertEquals("{\"active\":true}", JsonRepair.repair("{active: true}"));
        assertEquals("{\"disabled\":false}", JsonRepair.repair("{disabled: false}"));
        assertEquals("{\"value\":null}", JsonRepair.repair("{value: null}"));
    }
} 
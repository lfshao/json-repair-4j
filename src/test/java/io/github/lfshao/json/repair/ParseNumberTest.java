package io.github.lfshao.json.repair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 基于Python test_parse_number.py的Java测试类
 */
public class ParseNumberTest {

    @Test
    public void testParseNumber() {
        assertEquals("1", JsonRepair.repair("1"));
        assertEquals("1.2", JsonRepair.repair("1.2"));
    }

    @Test
    public void testParseNumberEdgeCases() {
        assertEquals("{\"test_key\":[\"test_value\",\"test_value2\"]}",
                JsonRepair.repair(" - { \"test_key\": [\"test_value\", \"test_value2\"] }"));

        assertEquals("{\"key\":\"1/3\"}",
                JsonRepair.repair("{\"key\": 1/3}"));

        assertEquals("{\"key\":0.25}",
                JsonRepair.repair("{\"key\": .25}"));

        assertEquals("{\"here\":\"now\",\"key\":\"1/3\",\"foo\":\"bar\"}",
                JsonRepair.repair("{\"here\": \"now\", \"key\": 1/3, \"foo\": \"bar\"}"));

        assertEquals("{\"key\":\"12345/67890\"}",
                JsonRepair.repair("{\"key\": 12345/67890}"));

        assertEquals("[105,12]",
                JsonRepair.repair("[105,12"));

        assertEquals("{\"key\":\"105,12\"}",
                JsonRepair.repair("{\"key\", 105,12,"));

        assertEquals("{\"key\":\"1/3\",\"foo\":\"bar\"}",
                JsonRepair.repair("{\"key\": 1/3, \"foo\": \"bar\"}"));

        assertEquals("{\"key\":\"10-20\"}",
                JsonRepair.repair("{\"key\": 10-20}"));

        assertEquals("{\"key\":\"1.1.1\"}",
                JsonRepair.repair("{\"key\": 1.1.1}"));

        assertEquals("[]",
                JsonRepair.repair("[- "));

        assertEquals("{\"key\":1.0}",
                JsonRepair.repair("{\"key\": 1. }"));

        assertEquals("{\"key\":1.0E10}",
                JsonRepair.repair("{\"key\": 1e10 }"));

        assertEquals("{\"key\":1}",
                JsonRepair.repair("{\"key\": 1e }"));

        assertEquals("{\"key\":\"1notanumber\"}",
                JsonRepair.repair("{\"key\": 1notanumber }"));

        assertEquals("[1,\"2notanumber\"]",
                JsonRepair.repair("[1, 2notanumber]"));
    }

    @Test
    public void testNegativeNumbers() {
        assertEquals("{\"key\":-5}", JsonRepair.repair("{\"key\": -5}"));
        assertEquals("{\"key\":-3.14}", JsonRepair.repair("{\"key\": -3.14}"));
        assertEquals("[-1,-2,-3]", JsonRepair.repair("[-1, -2, -3]"));
    }

    @Test
    public void testScientificNotation() {
        assertEquals("{\"key\":1.23E-4}", JsonRepair.repair("{\"key\": 1.23e-4}"));
        assertEquals("{\"key\":12300.0}", JsonRepair.repair("{\"key\": 1.23E4}"));
        assertEquals("{\"key\":1.0E10}", JsonRepair.repair("{\"key\": 1e10}"));
    }

    @Test
    public void testInvalidNumbers() {
        assertEquals("{\"key\":\"1.2.3\"}", JsonRepair.repair("{\"key\": 1.2.3}"));
        assertEquals("{\"key\":\"12abc\"}", JsonRepair.repair("{\"key\": 12abc}"));
        assertEquals("{\"key\":\"1..2\"}", JsonRepair.repair("{\"key\": 1..2}"));
    }

    @Test
    public void testLargeNumbers() {
        assertEquals("{\"key\":12345678901234567890}", JsonRepair.repair("{\"key\": 12345678901234567890}"));
        assertEquals("{\"key\":9223372036854775807}", JsonRepair.repair("{\"key\": 9223372036854775807}"));
    }


} 
package io.github.lfshao.json.repair;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonRepairTest {

	@Test
	void unescapedQuoteInString() {
		String in = "{\"foo\":\"abc\"def\"}";
		String out = JsonRepair.repair(in);
		assertEquals("{\"foo\":\"abc\\\"def\"}", out);
	}

	@Test
	void arithmeticExpression() {
		String in = "{\"number\": 1+3}";
		String out = JsonRepair.repair(in);
		assertEquals("{\"number\":4}", out);
	}

	@Test
	void unquotedKeysAndSingleQuotes() {
		String in = "{a:1, 'b': 'x'}";
		String out = JsonRepair.repair(in);
		assertEquals("{\"a\":1,\"b\":\"x\"}", out);
	}

	@Test
	void commentsRemoved() {
		String in = "/*c1*/{\n // c2\n a:1 // tail\n }";
		String out = JsonRepair.repair(in);
		assertEquals("{\"a\":1}", out);
	}

	@Test
	void specialLiteralsToNull() {
		String in = "{a: undefined, b: NaN, c: Infinity, d: -Infinity, e: None}";
		String out = JsonRepair.repair(in);
		assertEquals("{\"a\":null,\"b\":null,\"c\":null,\"d\":null,\"e\":null}", out);
	}

	@Test
	void weirdNumbersNormalized() {
		String in = "{a:.5, b:1., c:+1, d:1_000, e:2.5000}";
		String out = JsonRepair.repair(in);
		assertEquals("{\"a\":0.5,\"b\":1,\"c\":1,\"d\":1000,\"e\":2.5}", out);
	}

	@Test
	void topLevelMultipleValuesWrappedArray() {
		String in = "{a:1}{b:2}";
		String out = JsonRepair.repair(in);
		assertEquals("[{\"a\":1},{\"b\":2}]", out);
	}

	@Test
	void arrayTrailingComma() {
		String in = "[1,2,]";
		String out = JsonRepair.repair(in);
		assertEquals("[1,2]", out);
	}

	@Test
	void divideByZeroBecomesNull() {
		String in = "{n:1/0}";
		String out = JsonRepair.repair(in);
		assertEquals("{\"n\":null}", out);
	}

	@Test
	void markdownInlineJson() {
		String in = "`{\"foo\":\"bar\"}`";
		String out = JsonRepair.repair(in);
		assertEquals("{\"foo\":\"bar\"}", out);
	}

	@Test
	void markdownFencedJson() {
		String in = "```json\n{\"foo\":\"bar\"}\n```";
		String out = JsonRepair.repair(in);
		assertEquals("{\"foo\":\"bar\"}", out);
	}
} 
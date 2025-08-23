/*
 * Copyright (c) 2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.lfshao.json.repair.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal engine that repairs a malformed JSON-like string into RFC 8259 compliant JSON.
 * <p>
 * This implementation follows a tolerant scan with structural repair on the fly. It does not
 * depend on any third-party library.
 * </p>
 */
public final class RepairEngine {

	public RepairEngine() {
	}

	/**
	 * Repairs a possibly malformed JSON-like input into valid JSON.
	 *
	 * @param input raw input
	 * @return minified valid JSON string
	 */
	public String repair(String input) {
		String pre = stripMarkdownCodeFencesOrInline(input == null ? "" : input);
		char[] src = sanitizeInput(pre).toCharArray();
		Scanner s = new Scanner(src);
		List<String> topValues = new ArrayList<>();
		while (true) {
			s.skipSpacesAndComments();
			if (s.eof()) {
				break;
			}
			String v = parseValue(s);
			if (v == null) {
				// As a last resort, turn the next non-space token into a string
				String text = s.readUntilDelimiter();
				v = quoteString(text);
			}
			topValues.add(v);
			s.skipSpacesAndComments();
		}
		if (topValues.isEmpty()) {
			return "null";
		}
		if (topValues.size() == 1) {
			return topValues.get(0);
		}
		StringBuilder out = new StringBuilder();
		out.append('[');
		for (int i = 0; i < topValues.size(); i++) {
			if (i > 0) {
				out.append(',');
			}
			out.append(topValues.get(i));
		}
		out.append(']');
		return out.toString();
	}

	private String stripMarkdownCodeFencesOrInline(String input) {
		if (input == null) return "";
		String s = input.trim();
		// Inline single backticks: `...`
		if (s.length() >= 2 && s.charAt(0) == '`' && s.charAt(s.length() - 1) == '`') {
			String inner = s.substring(1, s.length() - 1).trim();
			if ((inner.startsWith("{") && inner.endsWith("}")) || (inner.startsWith("[") && inner.endsWith("]"))) {
				return inner;
			}
		}
		// Fenced code block with triple backticks
		if (s.contains("```")) {
			String norm = s.replace("\r\n", "\n").replace("\r", "\n");
			String[] lines = norm.split("\n", -1);
			int open = -1;
			for (int i = 0; i < lines.length; i++) {
				String ln = lines[i].trim();
				if (ln.startsWith("```") && ln.length() >= 3) { open = i; break; }
			}
			if (open >= 0) {
				int close = -1;
				for (int j = open + 1; j < lines.length; j++) {
					String ln = lines[j].trim();
					if (ln.startsWith("```") && ln.length() >= 3) { close = j; break; }
				}
				if (close > open) {
					StringBuilder body = new StringBuilder();
					for (int k = open + 1; k < close; k++) {
						if (k > open + 1) body.append('\n');
						body.append(lines[k]);
					}
					return body.toString().trim();
				}
			}
		}
		return input;
	}

	private String parseValue(Scanner s) {
		s.skipSpacesAndComments();
		if (s.eof()) return null;
		char c = s.peek();
		if (c == '{') {
			return parseObject(s);
		} else if (c == '[') {
			return parseArray(s);
		} else if (c == '"' || c == '\'' || c == '`') {
			return parseString(s);
		} else if (c == 't' || c == 'T' || c == 'f' || c == 'F' || c == 'n' || c == 'N' || c == 'I' || c == 'u' || c == 'U') {
			return parseLiteralOrIdentifierAsValue(s);
		} else if (isNumberOrExpressionStart(s)) {
			return parseNumberOrExpression(s);
		} else {
			// Fallback: try to read as identifier-ish and quote
			String word = s.readUntilDelimiter();
			return quoteString(word);
		}
	}

	private String parseObject(Scanner s) {
		StringBuilder out = new StringBuilder();
		s.expect('{');
		out.append('{');
		boolean first = true;
		while (!s.eof()) {
			s.skipSpacesAndComments();
			if (s.eof()) break;
			char c = s.peek();
			if (c == '}') {
				s.next();
				// close object
				out.append('}');
				return out.toString();
			}
			// If stray comma present, consume and continue
			if (c == ',') { s.next(); s.skipSpacesAndComments(); continue; }
			// Key
			String key;
			if (c == '"' || c == '\'' || c == '`') {
				key = parseString(s);
			} else if (isIdentifierStart(c)) {
				key = quoteString(parseUnquotedKey(s));
			} else {
				// unexpected token before a key; skip it and continue
				s.next();
				continue;
			}
			if (!first) {
				out.append(',');
			}
			first = false;
			out.append(key);
			s.skipSpacesAndComments();
			if (s.peekOrEOF() == ':') {
				s.next();
				out.append(':');
			} else {
				out.append(':');
			}
			// Value
			String val = parseValue(s);
			if (val == null) {
				val = "null";
			}
			out.append(val);
			// After value, normalize comma handling
			s.skipSpacesAndComments();
			if (s.peekOrEOF() == ',') {
				// consume one input comma and rely on our already appended comma before next pair
				s.next();
			}
		}
		// EOF; close object
		out.append('}');
		return out.toString();
	}

	private String parseArray(Scanner s) {
		StringBuilder out = new StringBuilder();
		s.expect('[');
		out.append('[');
		boolean first = true;
		while (!s.eof()) {
			s.skipSpacesAndComments();
			if (s.eof()) break;
			char c = s.peek();
			if (c == ']') {
				s.next();
				out.append(']');
				return out.toString();
			}
			// Skip stray commas
			if (c == ',') { s.next(); s.skipSpacesAndComments(); continue; }
			String val = parseValue(s);
			if (val == null) {
				val = "null";
			}
			if (!first) {
				out.append(',');
			}
			first = false;
			out.append(val);
			// After value, consume at most one comma from input
			s.skipSpacesAndComments();
			if (s.peekOrEOF() == ',') {
				s.next();
			}
		}
		out.append(']');
		return out.toString();
	}

	private String parseString(Scanner s) {
		char quote = s.next();
		StringBuilder content = new StringBuilder();
		while (!s.eof()) {
			char c = s.next();
			if (c == '\\') {
				if (s.eof()) {
					content.append('\\');
					break;
				}
				char esc = s.next();
				if (esc == 'x' || esc == 'X') {
					// \\xNN -> \\u00NN
					String h1 = s.has(2) ? new String(new char[]{s.peekAt(-1), s.peek()}) : ""; // placeholder; handled below robustly
					char hA = s.peekAt(0);
					char hB = s.peekAt(1);
					if (isHex(hA) && isHex(hB)) {
						s.next();
						s.next();
						appendUnicodeEscape(content, (hexVal(hA) << 4) + hexVal(hB));
					} else {
						content.append('\\').append('x');
					}
				} else if (esc == 'u' || esc == 'U') {
					int consumed = 0;
					int code = 0;
					for (int i = 0; i < 4; i++) {
						if (!s.eof() && isHex(s.peek())) {
							code = (code << 4) + hexVal(s.next());
							consumed++;
						} else {
							break;
						}
					}
					if (consumed == 4) {
						appendUnicodeEscape(content, code);
					} else {
						content.append('\\').append('u');
					}
				} else {
					// keep valid JSON escapes, fix others as \\u00XX
					switch (esc) {
						case '"': content.append('\\').append('"'); break;
						case '\\': content.append('\\').append('\\'); break;
						case '/': content.append('\\').append('/'); break;
						case 'b': case 'f': case 'n': case 'r': case 't':
							content.append('\\').append(esc);
							break;
						default:
							appendUnicodeEscape(content, esc);
					}
				}
				continue;
			}
			if (c == quote) {
				char la = s.peekNonSpaceOrEOF();
				if (la == ',' || la == '}' || la == ']' || la == ':' || la == 0) {
					// treat as closing quote
					break;
				} else {
					// treat as content, escape
					content.append('\\').append('"');
					continue;
				}
			}
			// if encountering a raw double quote inside other quote types, escape it
			if (c == '"' && quote != '"') {
				content.append('\\').append('"');
				continue;
			}
			if (c >= 0 && c < 32) {
				appendUnicodeEscape(content, c);
			} else {
				content.append(c);
			}
		}
		return '"' + content.toString() + '"';
	}

	private String parseLiteralOrIdentifierAsValue(Scanner s) {
		int start = s.pos;
		String word = s.readWord();
		if (word.isEmpty()) {
			return null;
		}
		String lower = word.toLowerCase();
		if (lower.equals("true") || lower.equals("false") || lower.equals("null")) {
			return lower;
		}
		if (lower.equals("none")) {
			return "null";
		}
		if (lower.equals("nan") || lower.equals("infinity") || lower.equals("-infinity") || lower.equals("undefined")) {
			return "null";
		}
		// not a known literal; treat as string value
		return quoteString(word);
	}

	private String parseNumberOrExpression(Scanner s) {
		int start = s.pos;
		String token = s.readNumberLikeToken();
		String trimmed = token.trim();
		if (trimmed.isEmpty()) return null;
		// handle sign followed by special identifiers like Infinity/NaN/Undefined
		if ((trimmed.equals("-") || trimmed.equals("+")) && Character.isLetter(s.peekOrEOF())) {
			String ident = s.readWord();
			String lower = ident.toLowerCase();
			if (lower.equals("infinity") || lower.equals("nan") || lower.equals("undefined")) {
				return "null";
			} else {
				// not recognized; push back behavior is not available, so best-effort: treat the sign+ident as string value
				return quoteString(trimmed + ident);
			}
		}
		if (looksLikeExpression(trimmed)) {
			Double val = ExpressionEvaluator.evaluate(trimmed);
			if (val == null || val.isNaN() || val.isInfinite()) {
				return "null";
			}
			return toJsonNumber(val);
		} else {
			String normalized = normalizeNumberLiteral(trimmed);
			if (normalized == null) return "null";
			return normalized;
		}
	}

	private String parseUnquotedKey(Scanner s) {
		StringBuilder sb = new StringBuilder();
		while (!s.eof()) {
			char c = s.peek();
			if (c == ':' || c == '}' || c == ',' || Character.isWhitespace(c)) {
				break;
			}
			if (c == '"' || c == '\'' || c == '`' || c == '{' || c == '[') {
				break;
			}
			sb.append(c);
			s.next();
		}
		return sb.toString().trim();
	}

	private boolean looksLikeExpression(String text) {
		boolean hasOp = false;
		boolean inExp = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == 'e' || c == 'E') {
				// exponent markers for number literal
				inExp = true;
				continue;
			}
			if (inExp && (c == '+' || c == '-')) {
				continue;
			}
			inExp = false;
			if (c == '*' || c == '/' || c == '%' || c == '(' || c == ')') {
				hasOp = true;
				break;
			}
			if ((c == '+' || c == '-') && i > 0) {
				// a plus/minus not at start and not part of exponent
				hasOp = true;
				break;
			}
		}
		return hasOp;
	}

	private String normalizeNumberLiteral(String raw) {
		String t = raw.replace("_", "");
		// if contains invalid letters other than e/E
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (!(c == '+' || c == '-' || c == '.' || (c >= '0' && c <= '9') || c == 'e' || c == 'E')) {
				return null;
			}
		}
		// leading +
		if (t.startsWith("+")) t = t.substring(1);
		// .5 -> 0.5
		if (t.startsWith(".")) t = "0" + t;
		// 1. -> 1
		if (t.endsWith(".")) t = t.substring(0, t.length() - 1);
		// handle lone sign or empty
		if (t.isEmpty() || t.equals("+") || t.equals("-") || t.equals("-")) return null;
		// Validate by parsing
		try {
			BigDecimal bd = new BigDecimal(t);
			// Emit minimal plain string
			return bd.stripTrailingZeros().toPlainString();
		} catch (Exception ex) {
			return null;
		}
	}

	private String toJsonNumber(Double d) {
		BigDecimal bd = BigDecimal.valueOf(d);
		return bd.stripTrailingZeros().toPlainString();
	}

	private void removeTrailingComma(StringBuilder out) {
		int i = out.length() - 1;
		while (i >= 0 && Character.isWhitespace(out.charAt(i))) i--;
		if (i >= 0 && out.charAt(i) == ',') {
			out.deleteCharAt(i);
		}
	}

	private static String quoteString(String s) {
		StringBuilder sb = new StringBuilder();
		sb.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"' || c == '\\') {
				sb.append('\\').append(c);
			} else if (c == '/' ) {
				sb.append('\\').append('/');
			} else if (c >= 0 && c < 32) {
				appendUnicodeEscape(sb, c);
			} else {
				sb.append(c);
			}
		}
		sb.append('"');
		return sb.toString();
	}

	private static void appendUnicodeEscape(StringBuilder sb, int code) {
		int c = code & 0xFF;
		sb.append('\\').append('u');
		String hex = Integer.toHexString(c);
		for (int i = hex.length(); i < 4; i++) sb.append('0');
		sb.append(hex);
	}

	private static boolean isHex(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	private static int hexVal(char c) {
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'a' && c <= 'f') return 10 + c - 'a';
		if (c >= 'A' && c <= 'F') return 10 + c - 'A';
		return 0;
	}

	private boolean isNumberOrExpressionStart(Scanner s) {
		char c = s.peek();
		return isNumberStart(c) || c == '(';
	}

	private boolean isNumberStart(char c) {
		return (c >= '0' && c <= '9') || c == '.' || c == '+' || c == '-';
	}

	private boolean isIdentifierStart(char c) {
		return Character.isLetter(c) || c == '_' || c == '$';
	}

	private String sanitizeInput(String input) {
		String s = input;
		if (s.length() > 0 && s.charAt(0) == '\uFEFF') {
			s = s.substring(1);
		}
		// normalize newlines
		s = s.replace("\r\n", "\n").replace("\r", "\n");
		// filter control chars except \t \n \r
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20 && !(c == '\t' || c == '\n' || c == '\r')) {
				// drop
				continue;
			}
			out.append(c);
		}
		return out.toString();
	}

	/**
	 * Simple tolerant scanner.
	 */
	static final class Scanner {
		final char[] src;
		int pos;
		final int len;

		Scanner(char[] src) {
			this.src = src;
			this.len = src.length;
			this.pos = 0;
		}

		boolean eof() { return pos >= len; }
		char peek() { return pos < len ? src[pos] : 0; }
		char peekOrEOF() { return pos < len ? src[pos] : 0; }
		char next() { return pos < len ? src[pos++] : 0; }
		boolean has(int n) { return pos + n < len; }
		char peekAt(int rel) { int i = pos + rel; return (i >= 0 && i < len) ? src[i] : 0; }

		void expect(char c) {
			if (!eof() && peek() == c) { next(); return; }
			// If not present, insert virtually by doing nothing
		}

		void skipSpacesAndComments() {
			while (!eof()) {
				char c = peek();
				if (Character.isWhitespace(c)) { next(); continue; }
				if (c == '/') {
					if (has(1) && peekAt(1) == '/') {
						// line comment
						next(); next();
						while (!eof() && peek() != '\n') next();
						continue;
					}
					if (has(1) && peekAt(1) == '*') {
						next(); next();
						while (!eof()) {
							char p = next();
							if (p == '*' && peekOrEOF() == '/') { next(); break; }
						}
						continue;
					}
				}
				break;
			}
		}

		String readUntilDelimiter() {
			StringBuilder sb = new StringBuilder();
			while (!eof()) {
				char c = peek();
				if (c == ',' || c == '}' || c == ']' || c == ':' || Character.isWhitespace(c)) break;
				sb.append(c);
				next();
			}
			return sb.toString().trim();
		}

		String readWord() {
			StringBuilder sb = new StringBuilder();
			while (!eof()) {
				char c = peek();
				if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '-') {
					sb.append(c);
					next();
				} else {
					break;
				}
			}
			return sb.toString();
		}

		String readNumberLikeToken() {
			StringBuilder sb = new StringBuilder();
			boolean inParens = false;
			while (!eof()) {
				char c = peek();
				if (c == '(') { inParens = true; }
				if (c == ')') { inParens = false; }
				if (c == ',' || c == '}' || c == ']' || (!inParens && c == ':')) break;
				if (Character.isWhitespace(c)) break;
				if (c == '/') {
					if (has(1) && (peekAt(1) == '/' || peekAt(1) == '*')) {
						break; // comment begins; stop token
					} else {
						sb.append(c);
						next();
						continue;
					}
				}
				if (Character.isLetter(c) && !(c == 'e' || c == 'E')) break;
				sb.append(c);
				next();
			}
			return sb.toString();
		}

		char peekNonSpaceOrEOF() {
			int i = pos;
			while (i < len && Character.isWhitespace(src[i])) i++;
			return i < len ? src[i] : 0;
		}
	}
} 
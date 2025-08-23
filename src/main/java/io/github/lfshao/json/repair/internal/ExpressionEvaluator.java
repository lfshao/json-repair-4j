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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A minimal, safe arithmetic expression evaluator for value context.
 * <p>
 * Supported grammar:
 * numbers (decimal, exponent), unary +/- , binary + - * / % , parentheses ().
 * Any identifier or other symbol causes failure (returns null).
 * </p>
 */
final class ExpressionEvaluator {

	private ExpressionEvaluator() {}

	static Double evaluate(String expr) {
		if (expr == null) return null;
		Tokenizer tz = new Tokenizer(expr);
		List<Token> output = new ArrayList<>();
		Deque<Token> ops = new ArrayDeque<>();
		Token prev = null;
		Token t;
		while ((t = tz.next()) != null) {
			switch (t.kind) {
				case NUMBER:
					output.add(t);
					break;
				case OP:
					// determine unary or binary for +/-
					Token opToken = t;
					if ((t.text.equals("+") || t.text.equals("-")) && (prev == null || prev.kind == TokenKind.OP || prev.kind == TokenKind.LPAREN)) {
						opToken = new Token(TokenKind.OP, t.text.equals("+") ? "u+" : "u-");
					}
					while (!ops.isEmpty() && ops.peek().kind == TokenKind.OP &&
							(precedence(ops.peek().text) > precedence(opToken.text) ||
								(precedence(ops.peek().text) == precedence(opToken.text) && isLeftAssoc(opToken.text)))) {
						output.add(ops.pop());
					}
					ops.push(opToken);
					break;
				case LPAREN:
					ops.push(t);
					break;
				case RPAREN:
					boolean matched = false;
					while (!ops.isEmpty()) {
						Token top = ops.pop();
						if (top.kind == TokenKind.LPAREN) { matched = true; break; }
						output.add(top);
					}
					if (!matched) return null;
					break;
				default:
					return null;
			}
			prev = t;
		}
		while (!ops.isEmpty()) {
			Token top = ops.pop();
			if (top.kind == TokenKind.LPAREN) return null;
			output.add(top);
		}
		// Evaluate RPN
		Deque<Double> st = new ArrayDeque<>();
		for (Token tok : output) {
			if (tok.kind == TokenKind.NUMBER) {
				try {
					st.push(Double.valueOf(tok.text));
				} catch (Exception ex) { return null; }
			} else if (tok.kind == TokenKind.OP) {
				if (tok.text.equals("u+") || tok.text.equals("u-")) {
					if (st.isEmpty()) return null;
					double a = st.pop();
					st.push(tok.text.equals("u+") ? +a : -a);
					continue;
				}
				if (st.size() < 2) return null;
				double b = st.pop();
				double a = st.pop();
				double r;
				switch (tok.text) {
					case "+": r = a + b; break;
					case "-": r = a - b; break;
					case "*": r = a * b; break;
					case "/": r = a / b; break;
					case "%": r = a % b; break;
					default: return null;
				}
				if (Double.isNaN(r) || Double.isInfinite(r)) return null;
				st.push(r);
			}
		}
		return st.size() == 1 ? st.pop() : null;
	}

	private static int precedence(String op) {
		switch (op) {
			case "u+": case "u-": return 4; // highest
			case "*": case "/": case "%": return 3;
			case "+": case "-": return 2;
			default: return 0;
		}
	}

	private static boolean isLeftAssoc(String op) {
		// unary are right-assoc
		return !(op.equals("u+") || op.equals("u-"));
	}

	private enum TokenKind { NUMBER, OP, LPAREN, RPAREN }

	private static final class Token {
		final TokenKind kind;
		final String text;
		Token(TokenKind kind, String text) { this.kind = kind; this.text = text; }
	}

	private static final class Tokenizer {
		final String s;
		int i;
		final int n;

		Tokenizer(String s) { this.s = s; this.i = 0; this.n = s.length(); }

		Token next() {
			skipSpaces();
			if (i >= n) return null;
			char c = s.charAt(i);
			if (isDigit(c) || c == '.') {
				// may be number or operator; prefer number if it parses
				int j = i;
				boolean sawDot = false; boolean sawExp = false;
				// parse number: digits [.] digits [e[+/-]digits]
				StringBuilder num = new StringBuilder();
				int p = j;
				while (p < n) {
					char ch = s.charAt(p);
					if (ch == '_') { p++; continue; }
					if (isDigit(ch)) { num.append(ch); p++; continue; }
					if (ch == '.') {
						if (sawDot) break; sawDot = true; num.append('.'); p++; continue;
					}
					if (ch == 'e' || ch == 'E') {
						if (sawExp) break; sawExp = true; num.append(ch); p++;
						if (p < n && (s.charAt(p) == '+' || s.charAt(p) == '-')) { num.append(s.charAt(p)); p++; }
						continue;
					}
					break;
				}
				if (num.length() > 0) {
					String txt = num.toString();
					i = p;
					return new Token(TokenKind.NUMBER, txt);
				}
				return null;
			}
			if (c == '+' || c == '-') { i++; return new Token(TokenKind.OP, Character.toString(c)); }
			if (c == '*' || c == '/' || c == '%') { i++; return new Token(TokenKind.OP, Character.toString(c)); }
			if (c == '(') { i++; return new Token(TokenKind.LPAREN, "("); }
			if (c == ')') { i++; return new Token(TokenKind.RPAREN, ")"); }
			// any other char is invalid
			return null;
		}

		void skipSpaces() {
			while (i < n) {
				char c = s.charAt(i);
				if (Character.isWhitespace(c)) { i++; } else { break; }
			}
		}

		boolean isDigit(char c) { return c >= '0' && c <= '9'; }
	}
} 
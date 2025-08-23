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
package io.github.lfshao.json.repair;

import io.github.lfshao.json.repair.internal.RepairEngine;

/**
 * A tolerant JSON string repair utility.
 * <p>
 * This class provides a single entry point {@link #repair(String)} that accepts a possibly malformed
 * JSON-like string and returns a strictly valid JSON string (RFC 8259 compliant). The implementation
 * performs a forgiving scan and structural fix, and finally emits a minified JSON string.
 * </p>
 */
public final class JsonRepair {

	private JsonRepair() {
		// Utility class; prevent instantiation.
	}

	/**
	 * Repairs a possibly malformed JSON-like input string into a valid JSON string.
	 * <ul>
	 *     <li>Removes comments, normalizes special literals, and filters illegal control characters.</li>
	 *     <li>Repairs strings (e.g., unescaped quotes, unfinished strings) and keys (auto-quote identifiers).</li>
	 *     <li>Repairs structure (missing commas/colons, trailing commas, unbalanced brackets).</li>
	 *     <li>Evaluates simple arithmetic expressions in value context. Invalid results become {@code null}.</li>
	 *     <li>Wraps multiple top-level values into an array.</li>
	 * </ul>
	 *
	 * @param input possibly malformed JSON string
	 * @return strict, minified JSON string
	 */
	public static String repair(String input) {
		if (input == null) {
			return "null";
		}
		return new RepairEngine().repair(input);
	}
} 
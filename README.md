# json-repair-4j

[中文说明 (Chinese)](README_CN.md)

A pure Java JSON string repair utility: take a possibly malformed JSON-like string and return a strictly valid (RFC 8259) JSON string.

## Features
- Tolerant repair capabilities:
  - Unescaped double quotes: naked `"` inside strings will be auto-escaped, unless it directly precedes a structural terminator (`, ] } :`) or the end of input
  - Single/backtick-quoted strings: normalized to standard double-quoted JSON strings
  - Unquoted object keys: auto-quoted
  - Missing/excess separators: missing colons/commas will be added; trailing commas removed
  - Unbalanced brackets: fixed or completed according to context
  - Comments: remove `// ...` and `/* ... */`
  - Special literals: `NaN`, `Infinity/-Infinity`, `undefined`, `None` → `null`
  - Number normalization: `.5→0.5`, `1.→1`, `+1→1`, `1_000→1000` (keeps valid exponent forms)
  - Escape repair: `\xNN` → `\u00NN`; control characters normalized to `\u00XX`
  - Expression evaluation: arithmetic expressions in value position (`+ - * / %`, unary `±`, parentheses) are safely evaluated; failure or `NaN/∞` → `null`
  - Multiple top-level values: wrapped into an array `[v1, v2, ...]`
  - Markdown awareness: automatically strips JSON from inline backticks or fenced code blocks

## Quick Start

### Installation (choose one)

- Maven Central (recommended, if released):

```xml
<dependency>
  <groupId>io.github.lfshao</groupId>
  <artifactId>json-repair-4j</artifactId>
  <version>0.1.0</version>
</dependency>
```

- Gradle (Groovy DSL):

```groovy
dependencies {
  implementation 'io.github.lfshao:json-repair-4j:0.1.0'
}
```

- Gradle (Kotlin DSL):

```kotlin
dependencies {
  implementation("io.github.lfshao:json-repair-4j:0.1.0")
}
```

### Usage Example

```java
import io.github.lfshao.json.repair.JsonRepair;

public class Demo {
    public static void main(String[] args) {
        String raw = "/* cmt */{a:1, 'b': 'x', s:\"abc\"def\" , n: 1+3}";
        String fixed = JsonRepair.repair(raw);
        System.out.println(fixed); // {"a":1,"b":"x","s":"abc\"def","n":4}
    }
}
```

Markdown input is supported as well (the fenced/inline code section will be extracted first):

```text
```json
{"foo":"bar"}
```
```

or:

```text
`{"foo":"bar"}`
```

Both will produce:

```json
{"foo":"bar"}
```

## Repair Rules (Details)
- Strings:
  - May start with `"`/`'`/`` ` ``; naked `"` inside is treated as content and auto-escaped unless it directly precedes `, ] } :` or end of input
  - Invalid/isolated escapes are converted to valid `\u00XX`
- Numbers:
  - Normalized, minimal output; remove underscores, fix leading/trailing dots and plus signs
- Expressions:
  - Only numbers and operators `+ - * / %`, parentheses `()`; unary `±`; standard precedence
  - Any identifiers/functions cause failure → `null`
  - Division by zero and overflow → `null`
- Structure:
  - Missing commas, trailing commas, missing colons, and missing closing brackets are repaired according to context
  - Multiple top-level values are wrapped into an array
- Comments and control characters:
  - Remove `//...` and `/*...*/`; filter invisible control chars (keep `\t\n\r`)

## Examples
- Unescaped double quote:
  - In: `{"s":"abc"def"}`
  - Out: `{"s":"abc\"def"}`
- Expression:
  - In: `{"n": (2+5)*0.5}`
  - Out: `{"n":3.5}`
- Unquoted key / single quotes:
  - In: `{a:1, 'b':'x'}`
  - Out: `{"a":1,"b":"x"}`
- Comments:
  - In: `/* c */{\n // c2\n a:1 // tail\n }`
  - Out: `{"a":1}`
- Multiple top-level values:
  - In: `{a:1}{b:2}`
  - Out: `[{"a":1},{"b":2}]`

## API
- `public final class JsonRepair`
  - `public static String repair(String input)`: repairs input to a valid JSON string (minified)

## Build & Test
- Run tests:

```bash
mvn test
```

## Constraints
- No third-party JSON libraries (JUnit 5 used only for tests)
- Output is guaranteed to be RFC 8259 compliant JSON

## License
Apache License 2.0. See `LICENSE` or `http://www.apache.org/licenses/LICENSE-2.0`. 
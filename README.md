# JSON Repair 4J

[中文版 README](README_CN.md) | English

A tool for repairing invalid JSON strings.

## Features

## Supported Repair Types

- ✅ **Missing Quotes**: Automatically add quotes for object keys and string values
- ✅ **Missing Separators**: Automatically add missing commas and colons
- ✅ **Missing Brackets**: Automatically complete missing braces and brackets
- ✅ **Escape Characters**: Properly handle escape sequences in strings
- ✅ **Unicode Characters**: Support Unicode characters (not escaped by default)
- ✅ **Comment Removal**: Remove single-line comments (`//`, `#`) and multi-line comments (`/* */`)
- ✅ **Mixed Quotes**: Support mixed single and double quotes
- ✅ **Number Format**: Fix invalid number formats, support large number handling
- ✅ **Boolean and null**: Fix incorrectly cased boolean and null values
- ✅ **Multi-JSON Merge**: Automatically merge multiple consecutive JSONs into an array

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>io.github.lfshao</groupId>
    <artifactId>json-repair-4j</artifactId>
    <version>0.3.0</version>
</dependency>
```

### Basic Usage

```java
import io.github.lfshao.json.repair.JsonRepair;

public class Example {
    public static void main(String[] args) {
        // Repair JSON with missing quotes
        String malformed = "{name: John, age: 30, city: New York}";
        String repaired = JsonRepair.repair(malformed);
        System.out.println(repaired);
        // Output: {"name":"John","age":30,"city":"New York"}

        // Repair JSON with missing brackets
        String incomplete = "[1, 2, 3, 4";
        String fixed = JsonRepair.repair(incomplete);
        System.out.println(fixed);
        // Output: [1,2,3,4]

        // Repair JSON with mixed issues
        String complex = "{name: 'John', items: [apple, banana, 'cherry'}";
        String result = JsonRepair.repair(complex);
        System.out.println(result);
        // Output: {"name":"John","items":["apple","banana","cherry"]}

        // Repair JSON with comments
        String withComments = "{\n  // User info\n  \"name\": \"John\",\n  \"age\": 30 /* age */\n}";
        String cleaned = JsonRepair.repair(withComments);
        System.out.println(cleaned);
        // Output: {"name":"John","age":30}

        // Repair and merge multiple JSONs
        String multiJson = "[1,2,3]{\"key\":\"value\"}";
        String merged = JsonRepair.repair(multiJson);
        System.out.println(merged);
        // Output: [[1,2,3],{"key":"value"}]
    }
}
```

## API Documentation

### JsonRepair.repair(String jsonStr)

Repairs malformed JSON strings.

**Parameters:**
- `jsonStr` - The JSON string to be repaired

**Returns:**
- A valid JSON string after repair

**Example:**
```java
String result = JsonRepair.repair("{name: John}");
// Returns: {"name":"John"}
```

## Implementation Principles

This tool is implemented based on the logic of the Python `json-repair` library, using a recursive descent parser:

1. **Lexical Analysis**: Character-by-character scanning of the input string
2. **Syntax Analysis**: Parse structure according to JSON grammar rules
3. **Error Repair**: Use heuristic rules to fix common errors
4. **Output Generation**: Generate standard JSON format strings

### Core Components

- `JsonParser`: Main parser that coordinates all sub-parsers
- `StringParser`: String parsing and repair
- `ObjectParser`: Object parsing and repair
- `ArrayParser`: Array parsing and repair
- `NumberParser`: Number parsing and repair
- `BooleanNullParser`: Boolean and null parsing
- `CommentParser`: Comment processing

## Test Cases

The project includes comprehensive test cases covering various JSON repair scenarios:

```bash
mvn test
```

## Building the Project

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package
mvn package
```

## License

Apache License 2.0

## Contributing

Issues and Pull Requests are welcome!

## Acknowledgments

This project is implemented based on the logic of the Python [json-repair](https://github.com/mangiucugna/json_repair) library. Thanks to the original author for their excellent work. 
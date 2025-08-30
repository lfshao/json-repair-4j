# JSON Repair 4J

中文版 | [English README](README.md)

这是一个用于修复无效的 JSON 字符串的工具。

## 特性

## 支持的修复类型

- ✅ **缺失引号**: 自动为对象键和字符串值添加引号
- ✅ **缺失分隔符**: 自动添加缺失的逗号和冒号
- ✅ **缺失括号**: 自动补全缺失的大括号和方括号
- ✅ **转义字符**: 正确处理字符串中的转义序列
- ✅ **Unicode字符**: 支持Unicode字符（默认不转义）
- ✅ **注释移除**: 移除单行注释（`//`, `#`）和多行注释（`/* */`）
- ✅ **混合引号**: 支持单引号、双引号混用的情况
- ✅ **数字格式**: 修复无效的数字格式，支持大数处理
- ✅ **布尔值和null**: 修复大小写不正确的布尔值和null值
- ✅ **多JSON合并**: 自动将多个连续的JSON合并为数组

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>io.github.lfshao</groupId>
    <artifactId>json-repair-4j</artifactId>
    <version>0.3.0</version>
</dependency>
```

### 基本用法

```java
import io.github.lfshao.json.repair.JsonRepair;

public class Example {
    public static void main(String[] args) {
        // 修复缺失引号的JSON
        String malformed = "{name: John, age: 30, city: New York}";
        String repaired = JsonRepair.repair(malformed);
        System.out.println(repaired);
        // 输出: {"name":"John","age":30,"city":"New York"}

        // 修复缺失括号的JSON
        String incomplete = "[1, 2, 3, 4";
        String fixed = JsonRepair.repair(incomplete);
        System.out.println(fixed);
        // 输出: [1,2,3,4]

        // 修复混合问题的JSON
        String complex = "{name: 'John', items: [apple, banana, 'cherry'}";
        String result = JsonRepair.repair(complex);
        System.out.println(result);
        // 输出: {"name":"John","items":["apple","banana","cherry"]}

        // 修复包含注释的JSON
        String withComments = "{\n  // 用户信息\n  \"name\": \"John\",\n  \"age\": 30 /* 年龄 */\n}";
        String cleaned = JsonRepair.repair(withComments);
        System.out.println(cleaned);
        // 输出: {"name":"John","age":30}

        // 修复多JSON合并
        String multiJson = "[1,2,3]{\"key\":\"value\"}";
        String merged = JsonRepair.repair(multiJson);
        System.out.println(merged);
        // 输出: [[1,2,3],{"key":"value"}]
    }
}
```

## API文档

### JsonRepair.repair(String jsonStr)

修复格式不正确的JSON字符串。

**参数:**
- `jsonStr` - 需要修复的JSON字符串

**返回值:**
- 修复后的有效JSON字符串

**示例:**
```java
String result = JsonRepair.repair("{name: John}");
// 返回: {"name":"John"}
```

## 实现原理

本工具基于Python版本的`json-repair`库的逻辑实现，采用递归下降解析器：

1. **词法分析**: 逐字符扫描输入字符串
2. **语法分析**: 根据JSON语法规则解析结构
3. **错误修复**: 使用启发式规则修复常见错误
4. **输出生成**: 生成标准JSON格式字符串

### 核心组件

- `JsonParser`: 主解析器，协调各个子解析器
- `StringParser`: 字符串解析和修复
- `ObjectParser`: 对象解析和修复
- `ArrayParser`: 数组解析和修复
- `NumberParser`: 数字解析和修复
- `BooleanNullParser`: 布尔值和null解析
- `CommentParser`: 注释处理

## 测试用例

项目包含全面的测试用例，覆盖各种JSON修复场景：

```bash
mvn test
```

## 构建项目

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 打包
mvn package
```

## 许可证

Apache License 2.0

## 贡献

欢迎提交Issue和Pull Request！
## 致谢

本项目基于Python版本的[json-repair](https://github.com/mangiucugna/json_repair)库的逻辑实现，感谢原作者的优秀工作。 
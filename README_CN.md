# json-repair-4j

一个纯 Java 的 JSON 字符串修复工具：输入可能不合法的 JSON 字符串，返回严格合法（RFC 8259）的 JSON 字符串。

## 特性
- 宽容修复能力：
  - 未转义的双引号：字符串内出现裸 `"` 会自动转义，除非紧邻结构结束符（`, ] } :` 或输入结束）
  - 单引号/反引号字符串：统一转为合法的双引号字符串
  - 未加引号的对象键：自动补上引号
  - 缺失/多余分隔：缺冒号、缺逗号会补；尾随逗号会移除
  - 括号不匹配：按上下文补齐或修正
  - 注释：移除 `// ...`、`/* ... */`
  - 特殊字面量：`NaN`、`Infinity/-Infinity`、`undefined`、`None` → `null`
  - 数字规范化：`.5→0.5`、`1.→1`、`+1→1`、`1_000→1000`（保留指数合法形式）
  - 转义修复：`\xNN` 转为 `\u00NN`；控制字符统一 `\u00XX`
  - 表达式求值：值位置的算术表达式（`+ - * / %`、一元 `±`、括号）将被安全求值；失败或 `NaN/∞` → `null`
  - 顶层多值：若出现多个顶层值，输出包成数组 `[v1, v2, ...]`
  - Markdown 识别：自动剥离行内反引号或三反引号围栏中的 JSON 内容

## 快速开始

### 安装（选择一种方式）

- Maven Central（推荐，若已发布）：

```xml
<dependency>
  <groupId>io.github.lfshao</groupId>
  <artifactId>json-repair-4j</artifactId>
  <version>0.1.0</version>
</dependency>
```

- Gradle（Groovy DSL）：

```groovy
dependencies {
  implementation 'io.github.lfshao:json-repair-4j:0.1.0'
}
```

- Gradle（Kotlin DSL）：

```kotlin
dependencies {
  implementation("io.github.lfshao:json-repair-4j:0.1.0")
}
```

### 使用示例

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

也支持 Markdown 输入（会先剥离围栏/行内代码）：

```text
```json
{"foo":"bar"}
```
```

或：

```text
`{"foo":"bar"}`
```

两者都会返回：

```json
{"foo":"bar"}
```

## 修复规则（细节）
- 字符串：
  - 起始引号可为 `"`/`'`/`` ` ``；内部裸 `"` 优先视为内容并自动转义，仅当后续紧邻 `, ] } :` 或输入结束时才视为闭合
  - 无效/孤立转义会被修复为合法的 `\u00XX`
- 数字：
  - 合法化与最小化输出，去掉多余下划线、修正前导/尾随小数点与正号
- 表达式：
  - 仅允许数字与运算符 `+ - * / %`、括号 `()`；一元 `±`；遵循标准优先级
  - 含标识符/函数名等将判为失败并输出 `null`
  - 除零与溢出 → `null`
- 结构：
  - 对象/数组的缺逗号、尾随逗号、缺冒号、缺闭合均按上下文修复
  - 多个顶层值会被包成数组
- 注释与控制字符：
  - 移除 `//...`、`/*...*/`；过滤不可见控制字符（保留 `\t\n\r`）

## 示例
- 未转义双引号：
  - 输入：`{"s":"abc"def"}`
  - 输出：`{"s":"abc\"def"}`
- 表达式：
  - 输入：`{"n": (2+5)*0.5}`
  - 输出：`{"n":3.5}`
- 未引号键/单引号：
  - 输入：`{a:1, 'b':'x'}`
  - 输出：`{"a":1,"b":"x"}`
- 注释：
  - 输入：`/* c */{\n // c2\n a:1 // tail\n }`
  - 输出：`{"a":1}`
- 顶层多值：
  - 输入：`{a:1}{b:2}`
  - 输出：`[{"a":1},{"b":2}]`

## API
- `public final class JsonRepair`
  - `public static String repair(String input)`：将输入修复为合法 JSON 字符串（minified）

## 构建与测试
- 构建/运行测试：

```bash
mvn test
```

## 约束
- 不依赖任何第三方 JSON 库（仅测试依赖 JUnit 5）
- 输出保证为 RFC 8259 合法 JSON

## 许可证
本项目以 Apache License 2.0 许可协议开源。查看仓库中的 `LICENSE` 文件或访问 `http://www.apache.org/licenses/LICENSE-2.0`。 
package io.github.lfshao.json.repair.core;

import io.github.lfshao.json.repair.parser.JsonElementParser;
import io.github.lfshao.json.repair.parser.impl.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JSON解析器核心类
 * 负责协调各个子解析器并管理解析状态
 */
public class JsonParser {

    // 字符串分隔符常量
    public static final List<Character> STRING_DELIMITERS = Arrays.asList('"', '\'', '“', '”');
    // 解析上下文
    private final JsonContext context;
    // 是否启用日志记录
    private final boolean logging;
    // 日志记录器
    private final List<LogEntry> logger;
    // 流稳定模式
    private final boolean streamStable;
    // 解析器注册表
    private final List<JsonElementParser> parsers;
    // 子解析器实例（用于内部调用）
    private final ArrayParser arrayParser;
    private final ObjectParser objectParser;
    private final StringParser stringParser;
    private final NumberParser numberParser;
    private final BooleanNullParser booleanNullParser;
    private final CommentParser commentParser;
    // 解析的字符串
    private String jsonStr;
    // 当前索引位置
    private int index;

    public JsonParser(String jsonStr, boolean logging, boolean streamStable) {
        this.jsonStr = jsonStr != null ? jsonStr : "";
        this.index = 0;
        this.context = new JsonContext();
        this.logging = logging;
        this.logger = logging ? new ArrayList<>() : null;
        this.streamStable = streamStable;

        // 初始化子解析器
        this.arrayParser = new ArrayParser(this);
        this.objectParser = new ObjectParser(this);
        this.stringParser = new StringParser(this);
        this.numberParser = new NumberParser(this);
        this.booleanNullParser = new BooleanNullParser(this);
        this.commentParser = new CommentParser(this);

        // 初始化解析器注册表（按优先级排序）
        this.parsers = Arrays.asList(
                this.objectParser,      // {
                this.arrayParser,       // [
                this.commentParser,     // # 或 /
                this.stringParser,      // 字符串和字母
                this.numberParser       // 数字
        );
    }

    /**
     * 开始解析JSON
     *
     * @return 解析结果
     */
    public Object parse() {
        Object json = parseJson();

        if (index < jsonStr.length()) {
            log("The parser returned early, checking if there's more json elements");
            List<Object> jsonList = new ArrayList<>();
            jsonList.add(json);

            while (index < jsonStr.length()) {
                Object j = parseJson();
                if (j != null && !"".equals(j)) {
                    if (!jsonList.isEmpty() && ObjectComparer.isSameObject(jsonList.get(jsonList.size() - 1), j)) {
                        // 用新的替换最后一个条目，因为新的似乎是更新
                        jsonList.remove(jsonList.size() - 1);
                    }
                    jsonList.add(j);
                } else {
                    // 这是一个失败，移动索引
                    index++;
                }
            }

            // 如果没有找到额外的内容，不返回数组
            if (jsonList.size() == 1) {
                log("There were no more elements, returning the element without the array");
                json = jsonList.get(0);
            } else {
                json = jsonList;
            }
        }

        return json;
    }

    /**
     * 解析单个JSON元素
     *
     * @return 解析的JSON元素
     */
    public Object parseJson() {
        while (true) {
            Character ch = getCharAt();

            // false表示我们已经到达提供的字符串的末尾
            if (ch == null) {
                return "";
            }

            // 使用责任链模式查找合适的解析器
            for (JsonElementParser parser : parsers) {
                if (parser.accept(ch, context)) {
                    // 对于对象和数组，需要先跳过开始字符
                    if (parser instanceof ObjectParser || parser instanceof ArrayParser) {
                        index++;
                    }
                    return parser.parse();
                }
            }

            // 如果没有解析器能处理，就忽略当前字符并继续
            index++;
        }
    }

    /**
     * 获取指定偏移位置的字符
     *
     * @param count 偏移量
     * @return 字符，如果超出范围返回null
     */
    public Character getCharAt(int count) {
        try {
            return jsonStr.charAt(index + count);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public Character getCharAt() {
        return getCharAt(0);
    }

    /**
     * 跳过空白字符
     *
     * @param idx           起始偏移
     * @param moveMainIndex 是否移动主索引
     * @return 跳过的字符数
     */
    public int skipWhitespacesAt(int idx, boolean moveMainIndex) {
        try {
            char ch = jsonStr.charAt(index + idx);
            while (Character.isWhitespace(ch)) {
                if (moveMainIndex) {
                    index++;
                } else {
                    idx++;
                }
                try {
                    ch = jsonStr.charAt(index + idx);
                } catch (IndexOutOfBoundsException e) {
                    return idx;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            return idx;
        }
        return idx;
    }

    /**
     * 跳过空白字符（移动主索引）
     */
    public void skipWhitespacesAt() {
        skipWhitespacesAt(0, true);
    }

    /**
     * 跳到指定字符
     *
     * @param character 目标字符
     * @param idx       起始偏移
     * @return 找到字符的位置偏移
     */
    public int skipToCharacter(char character, int idx) {
        return skipToCharacter(Collections.singletonList(character), idx);
    }

    /**
     * 跳到指定字符列表中的任一字符
     *
     * @param characters 目标字符列表
     * @param idx        起始偏移
     * @return 找到字符的位置偏移
     */
    public int skipToCharacter(List<Character> characters, int idx) {
        try {
            char ch = jsonStr.charAt(index + idx);
            while (!characters.contains(ch)) {
                idx++;
                try {
                    ch = jsonStr.charAt(index + idx);
                } catch (IndexOutOfBoundsException e) {
                    return idx;
                }
            }
            if (idx > 0 && jsonStr.charAt(index + idx - 1) == '\\') {
                // 这实际上是转义的，继续
                return skipToCharacter(characters, idx + 1);
            }
        } catch (IndexOutOfBoundsException e) {
            return idx;
        }
        return idx;
    }

    /**
     * 记录日志
     *
     * @param text 日志文本
     */
    public void log(String text) {
        if (logging && logger != null) {
            int window = 10;
            int start = Math.max(index - window, 0);
            int end = Math.min(index + window, jsonStr.length());
            String contextStr = jsonStr.substring(start, end);
            logger.add(new LogEntry(text, contextStr));
        }
    }

    // Getters
    public String getJsonStr() {
        return jsonStr;
    }

    public void setJsonStr(String jsonStr) {
        this.jsonStr = jsonStr;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public JsonContext getContext() {
        return context;
    }

    public boolean isLogging() {
        return logging;
    }

    public boolean isStreamStable() {
        return streamStable;
    }


    /**
     * 日志条目类
     */
    public static class LogEntry {
        private final String text;
        private final String context;

        public LogEntry(String text, String context) {
            this.text = text;
            this.context = context;
        }

        public String getText() {
            return text;
        }

        public String getContext() {
            return context;
        }
    }
} 
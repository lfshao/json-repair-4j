package io.github.lfshao.json.repair.parser;

import io.github.lfshao.json.repair.core.JsonContext;

/**
 * JSON元素解析器接口
 * 所有具体的解析器都应该实现此接口
 */
public interface JsonElementParser {

    /**
     * 解析JSON元素
     *
     * @return 解析后的对象
     */
    Object parse();

    /**
     * 检查当前字符是否可以被此解析器处理
     *
     * @param ch      当前字符（可能为null表示字符串结束）
     * @param context 解析上下文
     * @return 如果可以处理返回true
     */
    boolean accept(Character ch, JsonContext context);
} 
package io.github.lfshao.json.repair.core;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON解析上下文管理器
 * 用于跟踪当前解析位置的上下文信息
 */
public class JsonContext {

    private final List<ContextValues> context;
    private ContextValues current;
    private boolean empty;

    public JsonContext() {
        this.context = new ArrayList<>();
        this.current = null;
        this.empty = true;
    }

    /**
     * 设置新的上下文值
     *
     * @param value 要添加的上下文值
     */
    public void set(ContextValues value) {
        this.context.add(value);
        this.current = value;
        this.empty = false;
    }

    /**
     * 移除最近的上下文值
     */
    public void reset() {
        try {
            this.context.remove(this.context.size() - 1);
            if (!this.context.isEmpty()) {
                this.current = this.context.get(this.context.size() - 1);
            } else {
                this.current = null;
                this.empty = true;
            }
        } catch (IndexOutOfBoundsException e) {
            this.current = null;
            this.empty = true;
        }
    }

    public ContextValues getCurrent() {
        return current;
    }

    public boolean isEmpty() {
        return empty;
    }

    public List<ContextValues> getContext() {
        return new ArrayList<>(context);
    }

    public boolean contains(ContextValues value) {
        return context.contains(value);
    }

    public enum ContextValues {
        OBJECT_KEY,
        OBJECT_VALUE,
        ARRAY
    }
} 
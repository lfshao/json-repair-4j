package io.github.lfshao.json.repair;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对象比较器
 * 用于比较两个对象是否具有相同的结构
 */
public class ObjectComparer {

    /**
     * 递归比较两个对象，确保：
     * - 它们的类型匹配
     * - 它们的键/结构匹配
     *
     * @param obj1 第一个对象
     * @param obj2 第二个对象
     * @return 如果对象结构相同返回true，否则返回false
     */
    public static boolean isSameObject(Object obj1, Object obj2) {
        // 如果类型不匹配，立即返回false
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        if (!obj1.getClass().equals(obj2.getClass())) {
            return false;
        }

        if (obj1 instanceof Map) {
            Map<?, ?> map1 = (Map<?, ?>) obj1;
            Map<?, ?> map2 = (Map<?, ?>) obj2;

            // 检查两个都是Map且长度相同
            if (map1.size() != map2.size()) {
                return false;
            }

            for (Object key : map1.keySet()) {
                if (!map2.containsKey(key)) {
                    return false;
                }
                // 递归比较每个值
                if (!isSameObject(map1.get(key), map2.get(key))) {
                    return false;
                }
            }
            return true;
        } else if (obj1 instanceof List) {
            List<?> list1 = (List<?>) obj1;
            List<?> list2 = (List<?>) obj2;

            // 检查两个都是List且长度相同
            if (list1.size() != list2.size()) {
                return false;
            }

            // 递归比较每个元素
            for (int i = 0; i < list1.size(); i++) {
                if (!isSameObject(list1.get(i), list2.get(i))) {
                    return false;
                }
            }
            return true;
        }

        // 对于原子值：类型已经匹配，返回true
        return true;
    }

    /**
     * 如果值是空容器（字符串、列表、字典、集合等），返回true
     * 对于非容器类型如null、0、false等，返回false
     *
     * @param value 要检查的值
     * @return 如果是严格的空值返回true
     */
    public static boolean isStrictlyEmpty(Object value) {
        if (value instanceof String) {
            return ((String) value).isEmpty();
        } else if (value instanceof List) {
            return ((List<?>) value).isEmpty();
        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        } else if (value instanceof Set) {
            return ((Set<?>) value).isEmpty();
        }
        return false;
    }
} 
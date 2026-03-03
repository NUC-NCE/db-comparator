package com.datacheck.util;

import java.math.BigDecimal;

/**
 * 值比较工具类
 * 处理科学计数法和小数末尾0的差异比较
 */
public class ValueComparator {

    /**
     * 比较两个值是否相等（忽略科学计数法和小数末尾0的差异）
     */
    public static boolean equals(Object o1, Object o2) {
        if (o1 == o2) return true;
        if (o1 == null || o2 == null) return false;

        String s1 = normalize(o1.toString());
        String s2 = normalize(o2.toString());

        return s1.equals(s2);
    }

    /**
     * 标准化数值字符串：处理科学计数法和小数末尾0
     */
    private static String normalize(String value) {
        try {
            // 统一将大写E转为小写e
            String normalized = value.toUpperCase().replace("E", "e");
            BigDecimal bd = new BigDecimal(normalized);
            // 去除末尾的0
            bd = bd.stripTrailingZeros();
            return bd.toPlainString();
        } catch (NumberFormatException e) {
            // 非数值类型直接返回原值
            return value;
        }
    }
}

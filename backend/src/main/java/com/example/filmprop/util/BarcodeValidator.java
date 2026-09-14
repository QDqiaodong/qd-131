package com.example.filmprop.util;

/**
 * 道具条码校验工具。
 * 支持常见条码制式：EAN-13、EAN-8、UPC-A（纯数字，含校验位验证）、
 * Code 39（数字/大写字母及 - . 空格 $ / + %）、Code 128（可打印 ASCII）。
 */
public final class BarcodeValidator {

    /** 数据库 prop_code 列长度上限 */
    private static final int MAX_LENGTH = 50;
    private static final int CODE39_MIN = 3;
    private static final int CODE39_MAX = 43;
    private static final int CODE128_MIN = 4;
    private static final int CODE128_MAX = 48;

    private static final String CODE39_PATTERN = "[0-9A-Z\\-. $/+%]+";

    private BarcodeValidator() {
    }

    /**
     * 判断扫码枪解析出的码值是否为合法条码。
     */
    public static boolean isValid(String rawCode) {
        if (rawCode == null) {
            return false;
        }
        String code = rawCode.trim();
        if (code.isEmpty() || code.length() > MAX_LENGTH) {
            return false;
        }
        if (code.chars().allMatch(ch -> ch >= '0' && ch <= '9')) {
            // 纯数字码必须是带合法校验位的 EAN-13 / EAN-8 / UPC-A
            return isValidEan(code, 13) || isValidEan(code, 8) || isValidEan(code, 12);
        }
        if (code.matches(CODE39_PATTERN)
                && code.length() >= CODE39_MIN && code.length() <= CODE39_MAX) {
            return true;
        }
        // Code 128：可打印 ASCII
        return code.length() >= CODE128_MIN && code.length() <= CODE128_MAX
                && code.chars().allMatch(ch -> ch >= 0x20 && ch <= 0x7E);
    }

    /**
     * 校验指定长度的 EAN/UPC 校验位。
     * 从右往左（不含校验位）权重依次为 3、1、3、1……，
     * 即总长度为奇数时最左位权重为 1，偶数时最左位权重为 3。
     */
    private static boolean isValidEan(String code, int length) {
        if (code.length() != length) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < length - 1; i++) {
            int digit = code.charAt(i) - '0';
            boolean oddWeightOne = (length % 2 == 1);
            int weight = (i % 2 == 0) == oddWeightOne ? 1 : 3;
            sum += digit * weight;
        }
        int check = (10 - (sum % 10)) % 10;
        return check == code.charAt(length - 1) - '0';
    }
}

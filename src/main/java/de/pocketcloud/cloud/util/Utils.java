package de.pocketcloud.cloud.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Utils {

    public static float formatNumber(float number, int precision) {
        BigDecimal bd = BigDecimal.valueOf(number).setScale(precision, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float formatNumber(float number) {
        return formatNumber(number, 0);
    }

    public static float formatNumber(double number, int precision) {
        BigDecimal bd = BigDecimal.valueOf(number).setScale(precision, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float formatNumber(double number) {
        return formatNumber(number, 0);
    }
}
package com.calculator.application.services.utils;

public class MathUtils {

    public static double round2(double decimal) {
        return Math.round(decimal * 100.0) / 100.0;
    }

    public static double round4(double decimal) {
        return Math.round(decimal * 10000.0) / 10000.0;
    }

    public static double round6(double decimal) {
        return Math.round(decimal * 1000000.0) / 1000000.0;
    }

}

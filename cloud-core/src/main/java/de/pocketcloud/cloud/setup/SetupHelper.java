package de.pocketcloud.cloud.setup;

import de.pocketcloud.cloud.PocketCloud;

import java.text.NumberFormat;

public final class SetupHelper {

    public static String formatNumber(long number) {
        return NumberFormat.getInstance().format(number);
    }

    public static String formatNumber(int number) {
        return NumberFormat.getInstance().format(number);
    }

    public static String formatNumber(double number) {
        return NumberFormat.getInstance().format(number);
    }

    public static String parseTemplateName(String input, ErrorHolder error, boolean checkForExistence) {
        if (input == null || input.isBlank()) {
            error.set("Name cannot be empty!");
            return null;
        }

        if (PocketCloud.instance().templates().check(input) && checkForExistence) {
            error.set("A template with that name already exists!");
            return null;
        }

        if (!PocketCloud.instance().templates().checkName(input)) {
            error.set("Name must only contain letters, numbers, underscores and hyphens (no dots, slashes or special characters)!");
            return null;
        }

        return input.trim();
    }

    public static boolean parseBoolean(String input) {
        return input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes") ||
                input.equalsIgnoreCase("1") || input.equalsIgnoreCase("y");
    }

    public static Integer parseInt(String input, ErrorHolder error, int min, int max) {
        try {
            long value = Long.parseLong(input);
            if (value < min || value > max) throw new NumberFormatException();
            return (int) value;
        } catch (NumberFormatException | NullPointerException _) {
            error.set("Please enter a valid number between " + formatNumber(min) + " and " + formatNumber(max) + "!");
        }

        return null;
    }

    public static Long parseLong(String input, ErrorHolder error, long min, long max) {
        try {
            long value = Long.parseLong(input);
            if (value < min || value > max) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException | NullPointerException _) {
            error.set("Please enter a valid number between " + formatNumber(min) + " and " + formatNumber(max) + "!");
        }

        return null;
    }

    public static Double parseDouble(String input, ErrorHolder error, double min, double max, boolean percentage) {
        try {
            double value = Double.parseDouble(input);
            if (Double.isNaN(value) || Double.isInfinite(value) || value < min || value > max) {
                throw new NumberFormatException();
            }
            return percentage ? (value / 100) : value;
        } catch (NumberFormatException | NullPointerException _) {
            error.set("Please enter a valid decimal number between " + formatNumber(min) + " and " + formatNumber(max) + "!");
        }

        return null;
    }
}
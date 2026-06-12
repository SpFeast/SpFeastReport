package cn.spfeast.spfeastreport.util;

public final class DurationText {
    private DurationText() {
    }

    public static Long parseDurationMillis(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        long totalMillis = 0L;
        long currentNumber = 0L;
        boolean foundUnit = false;

        for (int i = 0; i < input.length(); i++) {
            char current = Character.toLowerCase(input.charAt(i));
            if (Character.isDigit(current)) {
                currentNumber = currentNumber * 10L + (current - '0');
                continue;
            }

            if (currentNumber <= 0L) {
                return null;
            }

            long multiplier = switch (current) {
                case 'w' -> 604_800_000L;
                case 'd' -> 86_400_000L;
                case 'h' -> 3_600_000L;
                case 'm' -> 60_000L;
                case 's' -> 1_000L;
                default -> -1L;
            };

            if (multiplier < 0L) {
                return null;
            }

            totalMillis += currentNumber * multiplier;
            currentNumber = 0L;
            foundUnit = true;
        }

        if (!foundUnit || currentNumber != 0L) {
            return null;
        }

        return totalMillis;
    }

    public static String formatDuration(long millis) {
        if (millis <= 0L) {
            return "0s";
        }

        long seconds = millis / 1000L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        StringBuilder builder = new StringBuilder();
        append(builder, days, "d");
        append(builder, hours, "h");
        append(builder, minutes, "m");
        append(builder, seconds, "s");
        return builder.toString().trim();
    }

    private static void append(StringBuilder builder, long value, String suffix) {
        if (value <= 0L) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(value).append(suffix);
    }
}

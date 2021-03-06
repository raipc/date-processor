package com.axibase.date;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class resolves creates for Axibase-supported datetime syntax. Each DatetimeProcessor object is immutable,
 * so consider caching them for better performance in client application.
 */
public class PatternResolver {
    private static final Pattern OPTIMIZED_PATTERN = Pattern.compile("yyyy-MM-dd('T'|T| )HH:mm:ss(\\.S[S]{0,8})?(Z{1,2}|'Z'|XXX)?");
    private static final Pattern DISABLE_LENIENT_MODE = Pattern.compile("^(?:u+|[^u]*u{1,3}[A-Za-z0-9]+)$");

    public static DatetimeProcessor createNewFormatter(String pattern) {
        if ("seconds".equalsIgnoreCase(pattern)) {
           return new DatetimeProcessorUnixSeconds();
        } else if ("milliseconds".equalsIgnoreCase(pattern)) {
           return new DatetimeProcessorUnixMillis();
        } else if ("tivoli".equalsIgnoreCase(pattern)) {
            return new DatetimeProcessorTivoli(false);
        } else if ("tivoli X".equalsIgnoreCase(pattern)) {
            return new DatetimeProcessorTivoli(true);
        } else if ("iso".equalsIgnoreCase(pattern)) {
            return new DatetimeProcessorIso8601(3, ZoneOffsetType.ISO8601);
        } else if ("MMM".equals(pattern)) {
            return new ShortMonthDateTimeProcessor(Locale.getDefault(Locale.Category.FORMAT));
        } else if ("MMMM".equals(pattern)) {
            return new FullMonthDatetimeProcessor(Locale.getDefault(Locale.Category.FORMAT));
        }
        final Matcher matcher = OPTIMIZED_PATTERN.matcher(pattern);
        if (matcher.matches()) {
            final int fractions = stringLength(matcher.group(2)) - 1;
            final ZoneOffsetType offsetType = ZoneOffsetType.byPattern(matcher.group(3));
            if (" ".equals(matcher.group(1))) {
                return new DatetimeProcessorLocal(fractions, offsetType);
            } else if (offsetType != ZoneOffsetType.NONE) {
                return new DatetimeProcessorIso8601(fractions, offsetType);
            }
        }
        final String preprocessedPattern = preprocessPattern(pattern);
        final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .parseCaseInsensitive();
        if (enableLenient(preprocessedPattern)) {
            builder.parseLenient();
        }
        final DateTimeFormatter dateTimeFormatter = builder
                .appendPattern(preprocessedPattern)
                .toFormatter(Locale.US)
                .withResolverStyle(ResolverStyle.STRICT);
        return new DatetimeProcessorCustom(dateTimeFormatter);
    }

    private static int stringLength(String value) {
        return value == null ? 0 : value.length();
    }

    private static boolean enableLenient(String pattern) {
        return !DISABLE_LENIENT_MODE.matcher(pattern).matches();
    }

    /**
     * Replace documented FDF symbols to their JSR-310 analogs. The convertions are performed:
     * unquoted T -> quoted T
     * u -> ccccc (day of week starting from Monday)
     * ZZ -> XX (zone offset in RFC format: +HHmm, Z for UTC)
     * ZZ -> XXX (zone offset in ISO format: +HH:mm, Z for UTC)
     * ZZZ -> VV (zone id)
     * @param pattern time formatting pattern
     * @return JSR-310 compatible pattern
     */
    private static String preprocessPattern(String pattern) {
        final int length = pattern.length();
        boolean insideQuotes = false;
        final StringBuilder sb = new StringBuilder(pattern.length() + 5);
        final DateFormatParsingState state = new DateFormatParsingState();
        for (int i = 0; i < length; i++) {
            final char c = pattern.charAt(i);
            if (c != 'u') {
                state.updateU(sb);
            }
            if (c != 'Z') {
                state.updateZ(sb);
            }
            switch (c) {
                case '\'':
                    insideQuotes = !insideQuotes;
                    sb.append(c);
                    break;
                case 'T':
                    if (!insideQuotes) {
                        sb.append("'T'");
                    } else {
                        sb.append(c);
                    }
                    break;
                case 'Z':
                    if (!insideQuotes) {
                        ++state.zCount;
                    }
                    sb.append(c);
                    break;
                case 'u':
                    if (!insideQuotes) {
                        ++state.uCount;
                    }
                    sb.append(c);
                    break;
                case 'y':
                    sb.append('u');
                    break;
                default:
                    sb.append(c);
            }
        }
        state.updateU(sb);
        state.updateZ(sb);
        return sb.toString();
    }

    private static final class DateFormatParsingState {
        private int zCount = 0;
        private int uCount = 0;

        private void updateU(StringBuilder sb) {
            if (uCount > 0) {
                sb.setLength(sb.length() - uCount);
                for (int i = 1; i < uCount; i++) {
                    sb.append('0');
                }
                sb.append("ccccc");
            }
            uCount = 0;
        }

        private void updateZ(StringBuilder sb) {
            if (zCount > 0 && zCount <= 3) {
                sb.setLength(sb.length() - zCount);
                if (zCount == 1) {
                    sb.append("XX");
                } else if (zCount == 2) {
                    sb.append("XXX");
                } else {
                    sb.append("VV");
                }
            }
            zCount = 0;
        }

    }
}

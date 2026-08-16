package tw.local.sixdigittoolbar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TitleTextParser {
    private static final Pattern SIX_DIGITS = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");

    private TitleTextParser() {
    }

    static String extractCode(String text) {
        Matcher matcher = SIX_DIGITS.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : null;
    }

    static String extractLikelyTitle(String text) {
        if (text == null) {
            return "";
        }

        String[] lines = text.replace('\r', '\n').split("\n");
        for (int i = 0; i < lines.length; i++) {
            String candidate = cleanLine(lines[i]);
            if (looksLikeJapaneseTitle(candidate)) {
                return candidate;
            }
        }

        for (int i = 0; i < lines.length; i++) {
            String candidate = cleanLine(lines[i]);
            if (looksLikeAnyTitle(candidate)) {
                return candidate;
            }
        }

        String compact = cleanLine(text);
        return looksLikeAnyTitle(compact) ? compact : "";
    }

    private static String cleanLine(String line) {
        if (line == null) {
            return "";
        }
        String text = line.trim();
        text = text.replaceAll("\\s+", " ");
        return text;
    }

    private static boolean looksLikeJapaneseTitle(String text) {
        if (!looksLikeAnyTitle(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(text.charAt(i));
            if (block == Character.UnicodeBlock.HIRAGANA
                    || block == Character.UnicodeBlock.KATAKANA
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeAnyTitle(String text) {
        if (text == null || text.length() < 4 || text.length() > 220) {
            return false;
        }
        if (SIX_DIGITS.matcher(text).matches()) {
            return false;
        }
        String lower = text.toLowerCase();
        return !lower.startsWith("http://")
                && !lower.startsWith("https://")
                && !lower.startsWith("tags:")
                && !lower.startsWith("artists:")
                && !lower.startsWith("groups:")
                && !lower.startsWith("languages:")
                && !lower.startsWith("parodies:")
                && !lower.contains("nhentai.net/g/");
    }
}

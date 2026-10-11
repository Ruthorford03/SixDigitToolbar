package tw.local.sixdigittoolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 同人誌與漫畫標題智慧解構器。
 * 負責從複雜的標籤字串中抽離核心作品名、作者/繪師、社團與原作。
 */
final class TitleDeconstructor {

    // 匹配開頭的活動括號：如 (C100), (COMIC1☆20), (サンクリ2023), (コミティア140) 等
    private static final Pattern PREFIX_EVENT = Pattern.compile("^\\s*\\([A-Za-z0-9\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\s☆★_\\-./]+\\)\\s*");

    // 匹配開頭作者/社團方括號：如 [社團 (作者)] 或 [作者]
    private static final Pattern PREFIX_BRACKET = Pattern.compile("^\\s*\\[([^\\]]+)\\]\\s*");

    // 匹配社團與作者格式：Circle (Artist)
    private static final Pattern CIRCLE_ARTIST = Pattern.compile("^(.+?)\\s*[\\(（](.+?)[\\)）]$");

    // 匹配結尾雜訊方括號：如 [DL版], [中国翻訳], [無修正], [漢化組] 等
    private static final Pattern SUFFIX_TAG = Pattern.compile("\\s*\\[([^\\]]+)\\]\\s*$");

    // 匹配結尾原作括號：如 (オリジナル), (Fate/Grand Order), (原神) 等
    private static final Pattern SUFFIX_PARODY = Pattern.compile("\\s*[\\(（]([^\\)）]+)[\\)）]\\s*$");

    static final class Result {
        final String rawTitle;
        final String cleanTitle;
        final String artist;
        final String circle;
        final String parody;
        final String simplifiedTitle;
        final List<String> candidateKeywords;

        Result(String rawTitle, String cleanTitle, String artist, String circle,
               String parody, String simplifiedTitle, List<String> candidateKeywords) {
            this.rawTitle = rawTitle == null ? "" : rawTitle;
            this.cleanTitle = cleanTitle == null ? "" : cleanTitle;
            this.artist = artist == null ? "" : artist;
            this.circle = circle == null ? "" : circle;
            this.parody = parody == null ? "" : parody;
            this.simplifiedTitle = simplifiedTitle == null ? "" : simplifiedTitle;
            this.candidateKeywords = candidateKeywords == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(candidateKeywords);
        }
    }

    private TitleDeconstructor() {
    }

    static Result deconstruct(String rawTitle) {
        if (rawTitle == null || rawTitle.trim().length() == 0) {
            return new Result("", "", "", "", "", "", Collections.<String>emptyList());
        }

        String working = rawTitle.trim();
        String circle = "";
        String artist = "";
        String parody = "";

        // 1. 剝離前綴活動標籤：(C100), (COMIC1), etc.
        Matcher eventMatcher = PREFIX_EVENT.matcher(working);
        if (eventMatcher.find()) {
            working = working.substring(eventMatcher.end()).trim();
        }

        // 2. 提取社團與作者方括號：[Circle (Artist)] 或 [Artist]
        Matcher bracketMatcher = PREFIX_BRACKET.matcher(working);
        if (bracketMatcher.find()) {
            String bracketContent = bracketMatcher.group(1).trim();
            Matcher caMatcher = CIRCLE_ARTIST.matcher(bracketContent);
            if (caMatcher.find()) {
                circle = caMatcher.group(1).trim();
                artist = caMatcher.group(2).trim();
            } else {
                // 若無小括號，通常為作者名或社團名
                artist = bracketContent;
            }
            working = working.substring(bracketMatcher.end()).trim();
        }

        // 3. 循環剝離結尾的後綴標記方括號：[DL版], [中国翻訳], [漢化], [無修正], etc.
        while (true) {
            Matcher suffixMatcher = SUFFIX_TAG.matcher(working);
            if (suffixMatcher.find()) {
                working = working.substring(0, suffixMatcher.start()).trim();
            } else {
                break;
            }
        }

        // 4. 檢查結尾原作括號 (Parody)
        Matcher parodyMatcher = SUFFIX_PARODY.matcher(working);
        if (parodyMatcher.find()) {
            String possibleParody = parodyMatcher.group(1).trim();
            // 避免把主標題後綴如 (上), (下), (1), (前編) 當成原作
            if (!isVolumeIndicator(possibleParody)) {
                parody = possibleParody;
                working = working.substring(0, parodyMatcher.start()).trim();
            }
        }

        String cleanTitle = working.trim();
        if (cleanTitle.length() == 0) {
            cleanTitle = rawTitle.trim();
        }

        // 5. 產生簡體中文候選
        String simplified = SimpleChineseConverter.toSimplified(cleanTitle);

        // 6. 彙整候選關鍵字清單 (循序去重)
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, cleanTitle);
        if (!simplified.equals(cleanTitle)) {
            addCandidate(candidates, simplified);
        }
        if (artist.length() > 0) {
            addCandidate(candidates, artist);
            String simplifiedArtist = SimpleChineseConverter.toSimplified(artist);
            if (!simplifiedArtist.equals(artist)) {
                addCandidate(candidates, simplifiedArtist);
            }
        }
        if (circle.length() > 0 && !circle.equals(artist)) {
            addCandidate(candidates, circle);
        }
        if (!rawTitle.equals(cleanTitle)) {
            addCandidate(candidates, rawTitle.trim());
        }

        return new Result(rawTitle, cleanTitle, artist, circle, parody, simplified, candidates);
    }

    private static boolean isVolumeIndicator(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.trim().toLowerCase();
        return lower.matches("^(?:上|中|下|前編|後編|完結編|総集編|[0-9]{1,3}|v(?:ol)?[0-9]{1,3}|ch(?:apter)?[0-9]{1,3})$");
    }

    private static void addCandidate(List<String> list, String item) {
        if (item != null && item.trim().length() > 0 && !list.contains(item.trim())) {
            list.add(item.trim());
        }
    }
}

package tw.local.sixdigittoolbar;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleFetchService extends IntentService {
    private static final String EXTRA_CODE = "code";
    private static final String EXTRA_URL = "url";
    private static final Pattern TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern JAPANESE_TITLE = Pattern.compile("<h2[^>]*class=[\"'][^\"']*title[^\"']*[\"'][^>]*>(.*?)</h2>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public TitleFetchService() {
        super("TitleFetchService");
    }

    static void fetch(Context context, String code, String url) {
        if (!HistoryStore.prepareTitleFetch(context, code, url)) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        final String stableCode = code;
        final String stableUrl = url;
        Intent intent = new Intent(appContext, TitleFetchService.class);
        intent.putExtra(EXTRA_CODE, code);
        intent.putExtra(EXTRA_URL, url);
        try {
            appContext.startService(intent);
        } catch (Exception ignored) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FetchResult result = fetchTitle(stableCode, stableUrl);
                    HistoryStore.upsert(appContext, stableCode, result.title, stableUrl, result.diagnostic);
                }
            }).start();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String code = intent.getStringExtra(EXTRA_CODE);
        String url = intent.getStringExtra(EXTRA_URL);
        FetchResult result = fetchTitle(code, url);
        HistoryStore.upsert(this, code, result.title, url, result.diagnostic);
    }

    private static FetchResult fetchTitle(String code, String urlText) {
        FetchResult apiV2 = fetchJapaneseTitleFromApi(code, true);
        if (isUsefulTitle(apiV2.title)) {
            return apiV2;
        }
        FetchResult apiV1 = fetchJapaneseTitleFromApi(code, false);
        if (isUsefulTitle(apiV1.title)) {
            return apiV1;
        }
        FetchResult html = fetchTitleFromHtml(urlText);
        if (isUsefulTitle(html.title)) {
            return html;
        }
        return new FetchResult("標題讀取失敗",
                joinDiagnostics(joinDiagnostics(apiV2.diagnostic, apiV1.diagnostic), html.diagnostic));
    }

    private static FetchResult fetchJapaneseTitleFromApi(String code, boolean v2) {
        if (code == null || code.length() == 0) {
            return new FetchResult("", "api: empty code");
        }

        HttpURLConnection connection = null;
        String label = v2 ? "apiV2" : "apiV1";
        try {
            URL url = new URL(v2
                    ? "https://nhentai.net/api/v2/galleries/" + code
                    : "https://nhentai.net/api/gallery/" + code);
            connection = openConnection(url);
            int status = connection.getResponseCode();
            String json = readResponse(connection, 30000);
            if (status < 200 || status >= 300) {
                return new FetchResult("", label + " HTTP " + status + " " + summarize(json));
            }
            JSONObject root = new JSONObject(json);
            JSONObject title = findTitleObject(root);
            if (title == null) {
                return new FetchResult("", label + " HTTP " + status + " missing title object " + summarize(json));
            }

            String japanese = title.optString("japanese", "").trim();
            if (japanese.length() > 0) {
                return new FetchResult(japanese, "");
            }

            String pretty = title.optString("pretty", "").trim();
            if (pretty.length() > 0) {
                return new FetchResult(pretty, "");
            }

            String english = title.optString("english", "").trim();
            return english.length() > 0
                    ? new FetchResult(english, "")
                    : new FetchResult("", label + " HTTP " + status + " title fields empty");
        } catch (Exception exception) {
            return new FetchResult("", label + " " + describe(exception));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JSONObject findTitleObject(JSONObject root) {
        JSONObject title = root.optJSONObject("title");
        if (title != null) {
            return title;
        }

        JSONObject data = root.optJSONObject("data");
        if (data != null) {
            title = data.optJSONObject("title");
            if (title != null) {
                return title;
            }
            JSONObject gallery = data.optJSONObject("gallery");
            if (gallery != null) {
                return gallery.optJSONObject("title");
            }
        }

        JSONObject gallery = root.optJSONObject("gallery");
        return gallery == null ? null : gallery.optJSONObject("title");
    }

    private static FetchResult fetchTitleFromHtml(String urlText) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlText);
            connection = openConnection(url);
            int status = connection.getResponseCode();
            String html = readResponse(connection, 60000);
            if (status < 200 || status >= 300) {
                return new FetchResult("", "html HTTP " + status + " " + summarize(html));
            }
            String title = parseTitle(html);
            if (isUsefulTitle(title)) {
                return new FetchResult(title, "");
            }
            return new FetchResult("", "html HTTP " + status + " no title " + summarize(html));
        } catch (Exception exception) {
            return new FetchResult("", "html " + describe(exception));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(8000);
        connection.setUseCaches(false);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept", "application/json,text/html,*/*");
        return connection;
    }

    private static String readResponse(HttpURLConnection connection, int maxLength) throws Exception {
        InputStream input = connection.getResponseCode() >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        if (input == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && body.length() < maxLength) {
            body.append(line).append('\n');
        }
        reader.close();
        return body.toString();
    }

    private static String parseTitle(String html) {
        Matcher japaneseMatcher = JAPANESE_TITLE.matcher(html == null ? "" : html);
        if (japaneseMatcher.find()) {
            String title = stripTags(japaneseMatcher.group(1));
            if (isUsefulTitle(title)) {
                return title;
            }
        }

        Matcher matcher = TITLE.matcher(html == null ? "" : html);
        if (!matcher.find()) {
            return "標題讀取失敗";
        }

        String title = decodeEntities(matcher.group(1).replace('\n', ' ').replace('\r', ' ').trim());
        title = title.replace(" » nhentai: hentai doujinshi and manga", "");
        title = title.replace(" - nhentai: hentai doujinshi and manga", "");
        return title.length() == 0 ? "標題讀取失敗" : title;
    }

    private static String stripTags(String html) {
        if (html == null) {
            return "";
        }
        return decodeEntities(html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim());
    }

    private static boolean isUsefulTitle(String title) {
        return title != null
                && title.trim().length() > 0
                && !"標題讀取中".equals(title)
                && !"標題讀取失敗".equals(title);
    }

    private static String decodeEntities(String text) {
        return text.replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private static String describe(Exception exception) {
        String message = exception.getMessage();
        String name = exception.getClass().getSimpleName();
        return message == null || message.length() == 0 ? name : name + ": " + sanitize(message);
    }

    private static String summarize(String text) {
        if (text == null) {
            return "";
        }
        String clean = sanitize(stripTags(text));
        return clean.length() > 180 ? clean.substring(0, 180) : clean;
    }

    private static String sanitize(String text) {
        return text.replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String joinDiagnostics(String first, String second) {
        if (first == null || first.length() == 0) {
            return second == null ? "" : second;
        }
        if (second == null || second.length() == 0) {
            return first;
        }
        return first + " | " + second;
    }

    private static final class FetchResult {
        final String title;
        final String diagnostic;

        FetchResult(String title, String diagnostic) {
            this.title = title == null ? "" : title;
            this.diagnostic = diagnostic == null ? "" : diagnostic;
        }
    }
}

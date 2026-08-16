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
        HistoryStore.upsert(context, code, "標題讀取中", url);
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
                    HistoryStore.upsert(appContext, stableCode, fetchTitle(stableCode, stableUrl), stableUrl);
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
        String title = fetchTitle(code, url);
        HistoryStore.upsert(this, code, title, url);
    }

    private static String fetchTitle(String code, String urlText) {
        String apiTitle = fetchJapaneseTitleFromApi(code);
        if (isUsefulTitle(apiTitle)) {
            return apiTitle;
        }
        return fetchTitleFromHtml(urlText);
    }

    private static String fetchJapaneseTitleFromApi(String code) {
        if (code == null || code.length() == 0) {
            return "";
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://nhentai.net/api/gallery/" + code);
            connection = openConnection(url);
            String json = readResponse(connection, 120000);
            JSONObject root = new JSONObject(json);
            JSONObject title = root.optJSONObject("title");
            if (title == null) {
                return "";
            }

            String japanese = title.optString("japanese", "").trim();
            if (japanese.length() > 0) {
                return japanese;
            }

            String pretty = title.optString("pretty", "").trim();
            if (pretty.length() > 0) {
                return pretty;
            }

            return title.optString("english", "").trim();
        } catch (Exception ignored) {
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String fetchTitleFromHtml(String urlText) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlText);
            connection = openConnection(url);
            String html = readResponse(connection, 120000);
            return parseTitle(html.toString());
        } catch (Exception ignored) {
            return "標題讀取失敗";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept", "application/json,text/html,*/*");
        return connection;
    }

    private static String readResponse(HttpURLConnection connection, int maxLength) throws Exception {
        InputStream input = connection.getInputStream();
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
}

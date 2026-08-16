package tw.local.sixdigittoolbar;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

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

    public TitleFetchService() {
        super("TitleFetchService");
    }

    static void fetch(Context context, String code, String url) {
        HistoryStore.upsert(context, code, "標題讀取中", url);
        Intent intent = new Intent(context, TitleFetchService.class);
        intent.putExtra(EXTRA_CODE, code);
        intent.putExtra(EXTRA_URL, url);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String code = intent.getStringExtra(EXTRA_CODE);
        String url = intent.getStringExtra(EXTRA_URL);
        String title = fetchTitle(url);
        HistoryStore.upsert(this, code, title, url);
    }

    private static String fetchTitle(String urlText) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlText);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            InputStream input = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && html.length() < 120000) {
                html.append(line).append('\n');
            }
            reader.close();
            return parseTitle(html.toString());
        } catch (Exception ignored) {
            return "標題讀取失敗";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String parseTitle(String html) {
        Matcher matcher = TITLE.matcher(html == null ? "" : html);
        if (!matcher.find()) {
            return "標題讀取失敗";
        }

        String title = decodeEntities(matcher.group(1).replace('\n', ' ').replace('\r', ' ').trim());
        title = title.replace(" » nhentai: hentai doujinshi and manga", "");
        title = title.replace(" - nhentai: hentai doujinshi and manga", "");
        return title.length() == 0 ? "標題讀取失敗" : title;
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

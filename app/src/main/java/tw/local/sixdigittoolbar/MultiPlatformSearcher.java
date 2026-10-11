package tw.local.sixdigittoolbar;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * 多平台中文漫畫搜尋與派發導航中樞 (Multi-Platform Search Hub)。
 * 整合 wnacg、JMComic、E-Hentai 中文區、Google 漢化聚合與電腦端派發。
 */
final class MultiPlatformSearcher {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private MultiPlatformSearcher() {
    }

    /**
     * wnacg 搜尋導航：
     * 若安裝有 wnacg App 則優先啟動貼上助手；若未安裝則直接開啟網頁版搜尋。
     */
    static void searchWnacg(Context context, String keyword) {
        if (isInvalid(keyword)) {
            toast(context, "沒有可搜尋的關鍵字");
            return;
        }

        String kw = keyword.trim();
        copyToClipboard(context, kw);

        // 先檢查是否有安裝 wnacg App
        if (hasWnacgApp(context)) {
            WnacgLauncher.search(context, kw);
        } else {
            // 未安裝 App，降級使用網頁版搜尋
            String url = "https://www.wnacg.com/search/index.php?q=" + encode(kw) + "&m=";
            openBrowser(context, url);
            toast(context, "已複製關鍵字並開啟 wnacg 網頁搜尋");
        }
    }

    /**
     * JMComic (禁漫天堂 / 18comic) 搜尋導航。
     */
    static void searchJmComic(Context context, String keyword) {
        if (isInvalid(keyword)) {
            toast(context, "沒有可搜尋的關鍵字");
            return;
        }

        String kw = keyword.trim();
        copyToClipboard(context, kw);
        String domain = UrlStore.getJmComicDomain(context);
        if (domain == null || domain.trim().length() == 0) {
            domain = "18comic.vip";
        }
        domain = domain.trim().replace("https://", "").replace("http://", "").replaceAll("/.*$", "");

        String url = "https://" + domain + "/search/photos?search_query=" + encode(kw);
        openBrowser(context, url);
        toast(context, "已開啟 JMComic 搜尋：" + kw);
    }

    /**
     * E-Hentai 中文區搜尋導航（附加 language:chinese 標籤）。
     */
    static void searchEHentai(Context context, String keyword) {
        if (isInvalid(keyword)) {
            toast(context, "沒有可搜尋的關鍵字");
            return;
        }

        String kw = keyword.trim();
        copyToClipboard(context, kw);
        String query = kw + " language:chinese";
        String url = "https://e-hentai.org/?f_search=" + encode(query);
        openBrowser(context, url);
        toast(context, "已開啟 E-Hentai 中文區搜尋：" + kw);
    }

    /**
     * Google 全網漢化組首發聚合搜尋。
     */
    static void searchGoogle(Context context, String keyword) {
        if (isInvalid(keyword)) {
            toast(context, "沒有可搜尋的關鍵字");
            return;
        }

        String kw = keyword.trim();
        copyToClipboard(context, kw);
        String query = "\"" + kw + "\" (漢化 OR 中文 OR 翻譯)";
        String url = "https://www.google.com/search?q=" + encode(query);
        openBrowser(context, url);
        toast(context, "已開啟 Google 全網漢化聚合搜尋");
    }

    /**
     * 派發代碼至電腦端 Sakura AI 漢化管線（路線 C 入口）。
     */
    static void dispatchToPc(final Context context, final String code, final String title) {
        if (code == null || code.trim().length() == 0) {
            toast(context, "無效的 6 位代碼");
            return;
        }

        final String dispatchUrl = UrlStore.getDispatchPcUrl(context);
        toast(context, "正在派發至電腦端管線 (" + code + ")...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(dispatchUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setDoOutput(true);

                    String payload = "{\"code\":\"" + escapeJson(code) + "\",\"title\":\"" + escapeJson(title) + "\"}";
                    OutputStream os = conn.getOutputStream();
                    os.write(payload.getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    final int codeResp = conn.getResponseCode();
                    conn.disconnect();

                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            if (codeResp >= 200 && codeResp < 300) {
                                toast(context, "✅ 已成功派發至電腦！背景開始下載與漢化");
                            } else {
                                toast(context, "⚠️ 電腦端回應狀態碼：" + codeResp);
                            }
                        }
                    });
                } catch (final Exception e) {
                    MAIN_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            toast(context, "❌ 連線電腦失敗，請確認電腦服務與區網網址是否正確 (" + e.getMessage() + ")");
                        }
                    });
                }
            }
        }).start();
    }

    private static boolean isInvalid(String s) {
        return s == null || s.trim().length() == 0 || "標題讀取中".equals(s) || "標題讀取失敗".equals(s);
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return URLEncoder.encode(s);
        }
    }

    private static void openBrowser(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            toast(context, "無法開啟瀏覽器：" + e.getMessage());
        }
    }

    private static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("漫畫搜尋關鍵字", text));
        }
    }

    private static boolean hasWnacgApp(Context context) {
        android.content.pm.PackageManager pm = context.getPackageManager();
        String[] knownPackages = new String[]{"com.wnacg", "com.wnacg.app", "com.wnacg.android"};
        for (int i = 0; i < knownPackages.length; i++) {
            if (pm.getLaunchIntentForPackage(knownPackages[i]) != null) {
                return true;
            }
        }
        return false;
    }

    private static void toast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

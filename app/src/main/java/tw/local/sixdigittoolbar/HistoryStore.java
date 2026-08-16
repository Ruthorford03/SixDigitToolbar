package tw.local.sixdigittoolbar;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class HistoryStore {
    private static final String PREFS = "history";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 50;

    private HistoryStore() {
    }

    static synchronized void upsert(Context context, String code, String title, String url) {
        upsert(context, code, title, url, "");
    }

    static synchronized void upsert(Context context, String code, String title, String url, String diagnostic) {
        if (code == null || code.length() == 0) {
            return;
        }

        ArrayList<HistoryEntry> current = new ArrayList<HistoryEntry>(list(context));
        ArrayList<HistoryEntry> next = new ArrayList<HistoryEntry>();
        next.add(new HistoryEntry(code, cleanTitle(title), url, cleanDiagnostic(diagnostic), System.currentTimeMillis()));

        for (int i = 0; i < current.size() && next.size() < MAX_ITEMS; i++) {
            HistoryEntry entry = current.get(i);
            if (!code.equals(entry.code)) {
                next.add(entry);
            }
        }

        save(context, next);
    }

    static synchronized boolean updateTitle(Context context, String code, String title) {
        if (code == null || code.length() == 0 || !isUsefulTitle(title)) {
            return false;
        }

        ArrayList<HistoryEntry> current = new ArrayList<HistoryEntry>(list(context));
        for (int i = 0; i < current.size(); i++) {
            HistoryEntry entry = current.get(i);
            if (code.equals(entry.code)) {
                current.set(i, new HistoryEntry(entry.code, title.trim(), entry.url, "", System.currentTimeMillis()));
                save(context, current);
                return true;
            }
        }
        return false;
    }

    static synchronized boolean updateMostRecentFailed(Context context, String title) {
        if (!isUsefulTitle(title)) {
            return false;
        }

        ArrayList<HistoryEntry> current = new ArrayList<HistoryEntry>(list(context));
        for (int i = 0; i < current.size(); i++) {
            HistoryEntry entry = current.get(i);
            if (!isUsefulTitle(entry.title)) {
                current.set(i, new HistoryEntry(entry.code, title.trim(), entry.url, "", System.currentTimeMillis()));
                save(context, current);
                return true;
            }
        }
        return false;
    }

    static synchronized List<HistoryEntry> list(Context context) {
        ArrayList<HistoryEntry> entries = new ArrayList<HistoryEntry>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                entries.add(new HistoryEntry(
                        object.optString("code"),
                        object.optString("title"),
                        object.optString("url"),
                        object.optString("diagnostic"),
                        object.optLong("timestamp")));
            }
        } catch (Exception ignored) {
        }
        return entries;
    }

    static synchronized void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ITEMS)
                .apply();
    }

    private static void save(Context context, List<HistoryEntry> entries) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < entries.size(); i++) {
            HistoryEntry entry = entries.get(i);
            JSONObject object = new JSONObject();
            try {
                object.put("code", entry.code);
                object.put("title", entry.title);
                object.put("url", entry.url);
                object.put("diagnostic", entry.diagnostic);
                object.put("timestamp", entry.timestamp);
                array.put(object);
            } catch (Exception ignored) {
            }
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ITEMS, array.toString())
                .apply();
    }

    private static String cleanTitle(String title) {
        if (title == null || title.trim().length() == 0) {
            return "標題讀取中";
        }
        return title.trim();
    }

    private static String cleanDiagnostic(String diagnostic) {
        if (diagnostic == null) {
            return "";
        }
        String clean = diagnostic.trim();
        return clean.length() > 500 ? clean.substring(0, 500) : clean;
    }

    static boolean isUsefulTitle(String title) {
        return title != null
                && title.trim().length() > 0
                && !"標題讀取中".equals(title)
                && !"標題讀取失敗".equals(title);
    }
}

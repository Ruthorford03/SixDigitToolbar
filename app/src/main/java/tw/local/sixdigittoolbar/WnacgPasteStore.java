package tw.local.sixdigittoolbar;

import android.content.Context;
import android.content.SharedPreferences;

final class WnacgPasteStore {
    private static final String PREFS = "wnacg_paste";
    private static final String KEY_TEXT = "text";
    private static final String KEY_TIME = "time";
    private static final long VALID_MS = 120000;
    private static boolean cacheLoaded;
    private static String cachedText = "";
    private static long cachedTime;

    private WnacgPasteStore() {
    }

    static synchronized void setPending(Context context, String text) {
        cachedText = text == null ? "" : text.trim();
        cachedTime = System.currentTimeMillis();
        cacheLoaded = true;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TEXT, cachedText)
                .putLong(KEY_TIME, cachedTime)
                .apply();
    }

    static synchronized String getPending(Context context) {
        if (!cacheLoaded) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            cachedText = prefs.getString(KEY_TEXT, "");
            cachedTime = prefs.getLong(KEY_TIME, 0);
            cacheLoaded = true;
        }
        if (System.currentTimeMillis() - cachedTime > VALID_MS) {
            clear(context);
            return "";
        }
        return cachedText;
    }

    static synchronized void clear(Context context) {
        cachedText = "";
        cachedTime = 0;
        cacheLoaded = true;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_TEXT)
                .remove(KEY_TIME)
                .apply();
    }
}

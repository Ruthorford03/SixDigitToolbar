package tw.local.sixdigittoolbar;

import android.content.Context;
import android.content.SharedPreferences;

final class WnacgPasteStore {
    private static final String PREFS = "wnacg_paste";
    private static final String KEY_TEXT = "text";
    private static final String KEY_TIME = "time";
    private static final long VALID_MS = 120000;

    private WnacgPasteStore() {
    }

    static void setPending(Context context, String text) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TEXT, text == null ? "" : text.trim())
                .putLong(KEY_TIME, System.currentTimeMillis())
                .apply();
    }

    static String getPending(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long time = prefs.getLong(KEY_TIME, 0);
        if (System.currentTimeMillis() - time > VALID_MS) {
            clear(context);
            return "";
        }
        return prefs.getString(KEY_TEXT, "");
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_TEXT)
                .remove(KEY_TIME)
                .apply();
    }
}

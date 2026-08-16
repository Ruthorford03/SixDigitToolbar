package tw.local.sixdigittoolbar;

import android.content.Context;
import android.content.SharedPreferences;

final class UrlStore {
    private static final String PREFS = "settings";
    private static final String KEY_BASE_URL = "base_url";

    private UrlStore() {
    }

    static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BASE_URL, context.getString(R.string.default_base_url));
    }

    static void setBaseUrl(Context context, String baseUrl) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BASE_URL, baseUrl.trim())
                .apply();
    }
}

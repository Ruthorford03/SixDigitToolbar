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

    private static final String KEY_JMCOMIC_DOMAIN = "jmcomic_domain";
    private static final String KEY_DISPATCH_PC_URL = "dispatch_pc_url";

    static String getJmComicDomain(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_JMCOMIC_DOMAIN, context.getString(R.string.default_jmcomic_domain));
    }

    static void setJmComicDomain(Context context, String domain) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_JMCOMIC_DOMAIN, domain.trim())
                .apply();
    }

    static String getDispatchPcUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DISPATCH_PC_URL, context.getString(R.string.default_dispatch_pc_url));
    }

    static void setDispatchPcUrl(Context context, String url) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DISPATCH_PC_URL, url.trim())
                .apply();
    }
}

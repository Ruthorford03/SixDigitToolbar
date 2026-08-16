package tw.local.sixdigittoolbar;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.util.List;

final class WnacgLauncher {
    private WnacgLauncher() {
    }

    static void search(Context context, String title) {
        if (title == null || title.trim().length() == 0 || "標題讀取中".equals(title) || "標題讀取失敗".equals(title)) {
            Toast.makeText(context, "沒有可搜尋的作品名稱", Toast.LENGTH_LONG).show();
            return;
        }

        copyToClipboard(context, title.trim());
        String packageName = findWnacgPackage(context);
        if (packageName == null) {
            Toast.makeText(context, "已複製作品名稱，但找不到 wnacg app", Toast.LENGTH_LONG).show();
            return;
        }

        if (!sendTextToPackage(context, packageName, title.trim())) {
            launchPackage(context, packageName);
        }
        Toast.makeText(context, "已複製作品名稱並開啟 wnacg", Toast.LENGTH_LONG).show();
    }

    private static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("作品名稱", text));
        }
    }

    private static boolean sendTextToPackage(Context context, String packageName, String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage(packageName);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(context.getPackageManager()) == null) {
                return false;
            }
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void launchPackage(Context context, String packageName) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
        }
    }

    private static String findWnacgPackage(Context context) {
        PackageManager pm = context.getPackageManager();
        String[] knownPackages = new String[] {"com.wnacg", "com.wnacg.app", "com.wnacg.android"};
        for (int i = 0; i < knownPackages.length; i++) {
            if (pm.getLaunchIntentForPackage(knownPackages[i]) != null) {
                return knownPackages[i];
            }
        }

        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(launcher, 0);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            String packageName = info.activityInfo.packageName == null ? "" : info.activityInfo.packageName;
            String label = info.loadLabel(pm) == null ? "" : info.loadLabel(pm).toString();
            String combined = (packageName + " " + label).toLowerCase();
            if (combined.contains("wnacg")) {
                return packageName;
            }
        }
        return null;
    }
}

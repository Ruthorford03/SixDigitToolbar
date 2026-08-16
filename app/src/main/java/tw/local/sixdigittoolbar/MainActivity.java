package tw.local.sixdigittoolbar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final Pattern SIX_DIGITS = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String selectedText = getIncomingText(getIntent());

        String code = extractSixDigitCode(selectedText);
        String title = TitleTextParser.extractLikelyTitle(selectedText);
        if (code == null) {
            if (HistoryStore.updateMostRecentFailed(this, title)) {
                Toast.makeText(this, "已用選取文字更新最近紀錄標題", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "請選取 6 位數字，或選取作品標題更新紀錄", Toast.LENGTH_LONG).show();
            }
            finish();
            return;
        }

        if (HistoryStore.isUsefulTitle(title) && HistoryStore.updateTitle(this, code, title)) {
            Toast.makeText(this, "已更新作品標題", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String url = buildTargetUrl(UrlStore.getBaseUrl(this), code);
        TitleFetchService.fetch(this, code, url);
        openBrowser(url);
        finish();
    }

    private static String getIncomingText(Intent intent) {
        CharSequence processText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if (processText != null) {
            return processText.toString();
        }

        CharSequence sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            return sharedText.toString();
        }

        return "";
    }

    static String extractSixDigitCode(String text) {
        Matcher matcher = SIX_DIGITS.matcher(text == null ? "" : text.trim());
        return matcher.find() ? matcher.group(1) : null;
    }

    static String buildTargetUrl(String baseUrl, String code) {
        String cleanBaseUrl = baseUrl == null || baseUrl.trim().isEmpty()
                ? "https://nhentai.net/g/"
                : baseUrl.trim();
        String encodedCode = Uri.encode(code);
        if (cleanBaseUrl.contains("{code}")) {
            return cleanBaseUrl.replace("{code}", encodedCode);
        }
        return cleanBaseUrl + encodedCode;
    }

    static void openBrowser(Activity activity, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, activity.getPackageName());
        activity.startActivity(intent);
    }

    private void openBrowser(String url) {
        openBrowser(this, url);
    }
}

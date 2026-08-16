package tw.local.sixdigittoolbar;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int padding = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("nhentai快捷");
        title.setTextSize(22);
        title.setGravity(Gravity.START);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("輸入基礎網址。可用 {code} 指定 6 位數放置位置；沒有 {code} 時會直接接在網址後方。");
        hint.setTextSize(15);
        hint.setPadding(0, dp(12), 0, dp(12));
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setText(UrlStore.getBaseUrl(this));
        urlInput.setSelectAllOnFocus(false);
        root.addView(urlInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button save = new Button(this);
        save.setText("儲存");
        save.setAllCaps(false);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = urlInput.getText().toString().trim();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    Toast.makeText(SettingsActivity.this, "網址必須以 http:// 或 https:// 開頭", Toast.LENGTH_LONG).show();
                    return;
                }
                UrlStore.setBaseUrl(SettingsActivity.this, url);
                Toast.makeText(SettingsActivity.this, "已儲存", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(16);
        root.addView(save, buttonParams);

        Button openClipboard = new Button(this);
        openClipboard.setText("前往剪貼簿代碼");
        openClipboard.setAllCaps(false);
        openClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = MainActivity.extractSixDigitCode(readClipboardText());
                if (code == null) {
                    Toast.makeText(SettingsActivity.this, "剪貼簿沒有 6 位數字", Toast.LENGTH_LONG).show();
                    return;
                }
                MainActivity.openBrowser(SettingsActivity.this,
                        MainActivity.buildTargetUrl(UrlStore.getBaseUrl(SettingsActivity.this), code));
            }
        });
        LinearLayout.LayoutParams clipboardButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        clipboardButtonParams.topMargin = dp(8);
        root.addView(openClipboard, clipboardButtonParams);

        setContentView(root);
    }

    private String readClipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            return "";
        }

        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0 || clip.getItemAt(0) == null) {
            return "";
        }

        CharSequence text = clip.getItemAt(0).coerceToText(this);
        return text == null ? "" : text.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

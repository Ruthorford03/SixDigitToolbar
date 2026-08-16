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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private LinearLayout historyContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int padding = dp(20);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

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
                String url = MainActivity.buildTargetUrl(UrlStore.getBaseUrl(SettingsActivity.this), code);
                TitleFetchService.fetch(SettingsActivity.this, code, url);
                MainActivity.openBrowser(SettingsActivity.this, url);
            }
        });
        LinearLayout.LayoutParams clipboardButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        clipboardButtonParams.topMargin = dp(8);
        root.addView(openClipboard, clipboardButtonParams);

        Button pasteTitle = new Button(this);
        pasteTitle.setText("用剪貼簿更新標題");
        pasteTitle.setAllCaps(false);
        pasteTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTitleFromClipboard(null);
            }
        });
        LinearLayout.LayoutParams pasteTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        pasteTitleParams.topMargin = dp(8);
        root.addView(pasteTitle, pasteTitleParams);

        addHistory(root);

        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderHistory();
    }

    private void addHistory(LinearLayout root) {
        TextView historyTitle = new TextView(this);
        historyTitle.setText("查詢紀錄");
        historyTitle.setTextSize(18);
        historyTitle.setPadding(0, dp(24), 0, dp(8));
        root.addView(historyTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        renderHistory();
    }

    private void renderHistory() {
        if (historyContainer == null) {
            return;
        }

        historyContainer.removeAllViews();
        List<HistoryEntry> entries = HistoryStore.list(this);
        if (entries.size() == 0) {
            TextView empty = new TextView(this);
            empty.setText("還沒有紀錄");
            empty.setTextSize(15);
            historyContainer.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            addHistoryRow(historyContainer, entries.get(i));
        }

        Button clear = new Button(this);
        clear.setText("清除紀錄");
        clear.setAllCaps(false);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HistoryStore.clear(SettingsActivity.this);
                recreate();
            }
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        clearParams.topMargin = dp(12);
        historyContainer.addView(clear, clearParams);
    }

    private void addHistoryRow(LinearLayout root, final HistoryEntry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView text = new TextView(this);
        text.setText(entry.code + "\n" + entry.title + "\n" + formatTime(entry.timestamp));
        text.setTextSize(15);
        row.addView(text, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(6), 0, 0);

        Button open = new Button(this);
        open.setText("前往");
        open.setAllCaps(false);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = entry.url == null || entry.url.length() == 0
                        ? MainActivity.buildTargetUrl(UrlStore.getBaseUrl(SettingsActivity.this), entry.code)
                        : entry.url;
                MainActivity.openBrowser(SettingsActivity.this, url);
            }
        });
        actions.addView(open, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button wnacg = new Button(this);
        wnacg.setText("搜尋wnacg");
        wnacg.setAllCaps(false);
        wnacg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WnacgLauncher.search(SettingsActivity.this, entry.title);
            }
        });
        actions.addView(wnacg, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button paste = new Button(this);
        paste.setText("貼上標題");
        paste.setAllCaps(false);
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTitleFromClipboard(entry.code);
            }
        });
        actions.addView(paste, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        row.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
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

    private void updateTitleFromClipboard(String code) {
        String clipboardText = readClipboardText();
        String title = TitleTextParser.extractLikelyTitle(clipboardText);
        if (!HistoryStore.isUsefulTitle(title)) {
            Toast.makeText(this, "剪貼簿沒有可用的作品標題", Toast.LENGTH_LONG).show();
            return;
        }

        boolean updated;
        if (code != null) {
            updated = HistoryStore.updateTitle(this, code, title);
        } else {
            String clipboardCode = TitleTextParser.extractCode(clipboardText);
            updated = clipboardCode != null
                    ? HistoryStore.updateTitle(this, clipboardCode, title)
                    : HistoryStore.updateMostRecentFailed(this, title);
        }

        if (updated) {
            Toast.makeText(this, "已更新標題", Toast.LENGTH_SHORT).show();
            renderHistory();
        } else {
            Toast.makeText(this, "找不到可更新的紀錄", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

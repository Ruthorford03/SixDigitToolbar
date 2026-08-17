package tw.local.sixdigittoolbar;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private static final int COLOR_BG = Color.rgb(246, 247, 251);
    private static final int COLOR_CARD = Color.WHITE;
    private static final int COLOR_TEXT = Color.rgb(25, 31, 42);
    private static final int COLOR_MUTED = Color.rgb(105, 113, 128);
    private static final int COLOR_BORDER = Color.rgb(224, 228, 236);
    private static final int COLOR_PRIMARY = Color.rgb(28, 101, 214);
    private static final int COLOR_PRIMARY_SOFT = Color.rgb(229, 239, 255);
    private static final int COLOR_SUCCESS = Color.rgb(20, 132, 84);
    private static final int COLOR_WARNING = Color.rgb(183, 106, 0);

    private LinearLayout historyContainer;
    private TextView clipboardStatus;
    private TextView clipboardHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP);
        root.setPadding(dp(16), dp(18), dp(16), dp(20));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addHeader(root);
        addPrimaryActions(root);
        addUrlSettings(root);
        addHistory(root);

        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateClipboardStatus();
        renderHistory();
    }

    private void addHeader(LinearLayout root) {
        LinearLayout card = makeCard();

        TextView title = new TextView(this);
        title.setText("nhentai快捷");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("複製 6 位數後，回來直接前往");
        subtitle.setTextColor(COLOR_MUTED);
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(4), 0, 0);
        card.addView(subtitle, matchWrap());

        clipboardStatus = new TextView(this);
        clipboardStatus.setTextColor(COLOR_PRIMARY);
        clipboardStatus.setTextSize(16);
        clipboardStatus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        clipboardStatus.setGravity(Gravity.CENTER_VERTICAL);
        clipboardStatus.setPadding(dp(12), dp(8), dp(12), dp(8));
        clipboardStatus.setBackground(roundedBg(COLOR_PRIMARY_SOFT, 18, 0, 0));
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(14);
        card.addView(clipboardStatus, statusParams);

        clipboardHint = new TextView(this);
        clipboardHint.setTextColor(COLOR_MUTED);
        clipboardHint.setTextSize(13);
        clipboardHint.setPadding(0, dp(8), 0, 0);
        card.addView(clipboardHint, matchWrap());

        root.addView(card, cardParams(0));
    }

    private void addPrimaryActions(LinearLayout root) {
        LinearLayout card = makeCard();

        Button openClipboard = makeButton("前往剪貼簿代碼", COLOR_PRIMARY, Color.WHITE);
        openClipboard.setTextSize(18);
        openClipboard.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        openClipboard.setMinHeight(dp(54));
        openClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = MainActivity.extractSixDigitCode(readClipboardText());
                if (code == null) {
                    Toast.makeText(SettingsActivity.this, "剪貼簿沒有 6 位數字", Toast.LENGTH_LONG).show();
                    updateClipboardStatus();
                    return;
                }
                String url = MainActivity.buildTargetUrl(UrlStore.getBaseUrl(SettingsActivity.this), code);
                TitleFetchService.fetch(SettingsActivity.this, code, url);
                MainActivity.openBrowser(SettingsActivity.this, url);
            }
        });
        card.addView(openClipboard, matchWrap());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button pasteTitle = makeButton("更新標題", Color.WHITE, COLOR_TEXT);
        pasteTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTitleFromClipboard(null);
            }
        });
        row.addView(pasteTitle, weightedButtonParams(0));

        Button accessibility = makeButton("貼上助手", Color.WHITE, COLOR_TEXT);
        accessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WnacgLauncher.openAccessibilitySettings(SettingsActivity.this);
            }
        });
        row.addView(accessibility, weightedButtonParams(dp(10)));

        LinearLayout.LayoutParams rowParams = matchWrap();
        rowParams.topMargin = dp(10);
        card.addView(row, rowParams);

        root.addView(card, cardParams(dp(12)));
    }

    private void addUrlSettings(LinearLayout root) {
        LinearLayout card = makeCard();

        final TextView toggle = new TextView(this);
        toggle.setText("設定網址");
        toggle.setTextColor(COLOR_TEXT);
        toggle.setTextSize(17);
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(toggle, matchWrap());

        final LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE);
        LinearLayout.LayoutParams panelParams = matchWrap();
        panelParams.topMargin = dp(10);
        card.addView(panel, panelParams);

        TextView hint = new TextView(this);
        hint.setText("目前：" + UrlStore.getBaseUrl(this) + "\n可用 {code} 指定 6 位數位置；沒有 {code} 時會直接接在網址後方。");
        hint.setTextColor(COLOR_MUTED);
        hint.setTextSize(13);
        panel.addView(hint, matchWrap());

        final EditText urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setText(UrlStore.getBaseUrl(this));
        urlInput.setSelectAllOnFocus(false);
        LinearLayout.LayoutParams inputParams = matchWrap();
        inputParams.topMargin = dp(8);
        panel.addView(urlInput, inputParams);

        Button save = makeButton("儲存網址", COLOR_PRIMARY, Color.WHITE);
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
        LinearLayout.LayoutParams saveParams = matchWrap();
        saveParams.topMargin = dp(10);
        panel.addView(save, saveParams);

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean show = panel.getVisibility() != View.VISIBLE;
                panel.setVisibility(show ? View.VISIBLE : View.GONE);
                toggle.setText(show ? "設定網址  收合" : "設定網址");
            }
        });

        root.addView(card, cardParams(dp(12)));
    }

    private void addHistory(LinearLayout root) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView historyTitle = new TextView(this);
        historyTitle.setText("查詢紀錄");
        historyTitle.setTextColor(COLOR_TEXT);
        historyTitle.setTextSize(20);
        historyTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(historyTitle, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button clear = makeButton("清除", Color.WHITE, COLOR_MUTED);
        clear.setMinHeight(dp(40));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HistoryStore.clear(SettingsActivity.this);
                renderHistory();
            }
        });
        header.addView(clear, new LinearLayout.LayoutParams(dp(88),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams headerParams = matchWrap();
        headerParams.topMargin = dp(20);
        root.addView(header, headerParams);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = matchWrap();
        containerParams.topMargin = dp(8);
        root.addView(historyContainer, containerParams);
    }

    private void renderHistory() {
        if (historyContainer == null) {
            return;
        }

        historyContainer.removeAllViews();
        List<HistoryEntry> entries = HistoryStore.list(this);
        if (entries.size() == 0) {
            LinearLayout emptyCard = makeCard();
            TextView empty = new TextView(this);
            empty.setText("還沒有紀錄");
            empty.setTextColor(COLOR_MUTED);
            empty.setTextSize(15);
            emptyCard.addView(empty, matchWrap());
            historyContainer.addView(emptyCard, cardParams(0));
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            addHistoryRow(historyContainer, entries.get(i));
        }
    }

    private void addHistoryRow(LinearLayout root, final HistoryEntry entry) {
        LinearLayout card = makeCard();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView code = new TextView(this);
        code.setText(entry.code);
        code.setTextColor(COLOR_TEXT);
        code.setTextSize(22);
        code.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(code, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView status = new TextView(this);
        status.setText(statusText(entry.title));
        status.setTextColor(statusColor(entry.title));
        status.setTextSize(12);
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(10), dp(5), dp(10), dp(5));
        status.setBackground(roundedBg(statusBg(entry.title), 14, 0, 0));
        top.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(top, matchWrap());

        TextView title = new TextView(this);
        title.setText(entry.title);
        title.setTextColor(HistoryStore.isUsefulTitle(entry.title) ? COLOR_TEXT : COLOR_MUTED);
        title.setTextSize(16);
        title.setMaxLines(3);
        title.setPadding(0, dp(8), 0, 0);
        card.addView(title, matchWrap());

        TextView time = new TextView(this);
        time.setText(formatTime(entry.timestamp));
        time.setTextColor(COLOR_MUTED);
        time.setTextSize(13);
        time.setPadding(0, dp(6), 0, 0);
        card.addView(time, matchWrap());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button open = makeButton("前往", Color.WHITE, COLOR_TEXT);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = entry.url == null || entry.url.length() == 0
                        ? MainActivity.buildTargetUrl(UrlStore.getBaseUrl(SettingsActivity.this), entry.code)
                        : entry.url;
                MainActivity.openBrowser(SettingsActivity.this, url);
            }
        });
        actions.addView(open, weightedButtonParams(0));

        Button wnacg = makeButton("搜尋wnacg", Color.WHITE, COLOR_TEXT);
        wnacg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WnacgLauncher.search(SettingsActivity.this, entry.title);
            }
        });
        actions.addView(wnacg, weightedButtonParams(dp(8)));

        Button paste = makeButton("貼上標題", Color.WHITE, COLOR_TEXT);
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTitleFromClipboard(entry.code);
            }
        });
        actions.addView(paste, weightedButtonParams(dp(8)));

        LinearLayout.LayoutParams actionsParams = matchWrap();
        actionsParams.topMargin = dp(12);
        card.addView(actions, actionsParams);

        if (entry.diagnostic != null && entry.diagnostic.length() > 0) {
            final TextView diagnosticText = new TextView(this);
            diagnosticText.setText("診斷：" + entry.diagnostic);
            diagnosticText.setTextColor(COLOR_MUTED);
            diagnosticText.setTextSize(12);
            diagnosticText.setVisibility(View.GONE);
            diagnosticText.setPadding(0, dp(6), 0, 0);

            TextView diagnosticToggle = new TextView(this);
            diagnosticToggle.setText("顯示診斷");
            diagnosticToggle.setTextColor(COLOR_PRIMARY);
            diagnosticToggle.setTextSize(13);
            diagnosticToggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            diagnosticToggle.setPadding(0, dp(10), 0, 0);
            diagnosticToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    diagnosticText.setVisibility(diagnosticText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
            });
            card.addView(diagnosticToggle, matchWrap());
            card.addView(diagnosticText, matchWrap());
        }

        root.addView(card, cardParams(dp(10)));
    }

    private String statusText(String title) {
        if ("標題讀取中".equals(title)) {
            return "讀取中";
        }
        if (HistoryStore.isUsefulTitle(title)) {
            return "已完成";
        }
        return "需補標題";
    }

    private int statusColor(String title) {
        if (HistoryStore.isUsefulTitle(title)) {
            return COLOR_SUCCESS;
        }
        return COLOR_WARNING;
    }

    private int statusBg(String title) {
        if (HistoryStore.isUsefulTitle(title)) {
            return Color.rgb(225, 246, 236);
        }
        return Color.rgb(255, 242, 219);
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

    private void updateClipboardStatus() {
        if (clipboardStatus == null) {
            return;
        }
        String code = MainActivity.extractSixDigitCode(readClipboardText());
        if (code == null) {
            clipboardStatus.setText("剪貼簿：未偵測到 6 位數");
            clipboardHint.setText("先複製代碼，再按下前往剪貼簿代碼。");
        } else {
            clipboardStatus.setText("剪貼簿：" + code);
            clipboardHint.setText("已準備好開啟 " + MainActivity.buildTargetUrl(UrlStore.getBaseUrl(this), code));
        }
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

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackground(roundedBg(COLOR_CARD, 14, COLOR_BORDER, 1));
        card.setElevation(dp(1));
        return card;
    }

    private Button makeButton(String text, int bgColor, int textColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTextColor(textColor);
        button.setMinHeight(dp(48));
        int borderColor = bgColor == Color.WHITE ? COLOR_BORDER : bgColor;
        button.setBackground(roundedBg(bgColor, 12, borderColor, 1));
        return button;
    }

    private GradientDrawable roundedBg(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams cardParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin;
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightedButtonParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.leftMargin = leftMargin;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

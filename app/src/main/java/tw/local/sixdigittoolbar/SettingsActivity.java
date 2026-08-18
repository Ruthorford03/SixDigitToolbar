package tw.local.sixdigittoolbar;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private static final long MOTION_FAST_MS = 100;
    private static final long MOTION_NORMAL_MS = 260;

    private LinearLayout historyContainer;
    private TextView clipboardStatus;
    private TextView clipboardHint;
    private String lastHistoryFingerprint;
    private final List<View> entranceViews = new ArrayList<View>();
    private final Interpolator motionInterpolator = new PathInterpolator(0.2f, 0f, 0f, 1f);
    private boolean motionEnabled;
    private ViewGroup rootContainer;
    private View entranceRoot;
    private Runnable entranceRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        motionEnabled = shouldUseMotion();
        configureWindowForAndroid16();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BG);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP);
        rootContainer = root;
        applySystemInsets(root);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addHeader(root);
        addPrimaryActions(root);
        addUrlSettings(root);
        addHistory(root);

        setContentView(scrollView);
        root.requestApplyInsets();
        playScreenEntrance(root);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (entranceRoot != null && entranceRunnable != null) {
            entranceRoot.removeCallbacks(entranceRunnable);
        }
        entranceRoot = null;
        entranceRunnable = null;
        resetScaleAnimations(rootContainer);
        for (int i = 0; i < entranceViews.size(); i++) {
            View view = entranceViews.get(i);
            view.animate().cancel();
            view.setAlpha(1f);
            view.setTranslationY(0f);
        }
        if (clipboardStatus != null) {
            clipboardStatus.animate().cancel();
            clipboardStatus.setAlpha(1f);
            clipboardStatus.setScaleX(1f);
            clipboardStatus.setScaleY(1f);
        }
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
        registerEntrance(card);
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
        registerEntrance(card);
    }

    private void addUrlSettings(LinearLayout root) {
        final LinearLayout card = makeCard();

        final TextView toggle = new TextView(this);
        toggle.setText("設定網址");
        toggle.setTextColor(COLOR_TEXT);
        toggle.setTextSize(17);
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        toggle.setPadding(0, dp(4), 0, dp(4));
        attachPressMotion(toggle);
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
                beginContentTransition(card);
                panel.setVisibility(show ? View.VISIBLE : View.GONE);
                toggle.setText(show ? "設定網址    收合" : "設定網址    展開");
            }
        });

        toggle.setText("設定網址    展開");

        root.addView(card, cardParams(dp(12)));
        registerEntrance(card);
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
        registerEntrance(header);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = matchWrap();
        containerParams.topMargin = dp(8);
        root.addView(historyContainer, containerParams);
        registerEntrance(historyContainer);
    }

    private void renderHistory() {
        if (historyContainer == null) {
            return;
        }

        List<HistoryEntry> entries = HistoryStore.list(this);
        String fingerprint = historyFingerprint(entries);
        if (fingerprint.equals(lastHistoryFingerprint)) {
            return;
        }

        if (lastHistoryFingerprint != null) {
            beginContentTransition(historyContainer);
        }
        lastHistoryFingerprint = fingerprint;
        historyContainer.removeAllViews();
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
        final LinearLayout card = makeCard();

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
        boolean stackActions = shouldStackHistoryActions();
        actions.setOrientation(stackActions ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

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
        actions.addView(open, historyActionParams(stackActions, 0));

        Button wnacg = makeButton("搜尋wnacg", Color.WHITE, COLOR_TEXT);
        wnacg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WnacgLauncher.search(SettingsActivity.this, entry.title);
            }
        });
        actions.addView(wnacg, historyActionParams(stackActions, dp(8)));

        Button paste = makeButton("貼上標題", Color.WHITE, COLOR_TEXT);
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTitleFromClipboard(entry.code);
            }
        });
        actions.addView(paste, historyActionParams(stackActions, dp(8)));

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

            final TextView diagnosticToggle = new TextView(this);
            diagnosticToggle.setText("顯示診斷");
            diagnosticToggle.setTextColor(COLOR_PRIMARY);
            diagnosticToggle.setTextSize(13);
            diagnosticToggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            diagnosticToggle.setPadding(0, dp(10), 0, 0);
            attachPressMotion(diagnosticToggle);
            diagnosticToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean show = diagnosticText.getVisibility() != View.VISIBLE;
                    beginContentTransition(card);
                    diagnosticText.setVisibility(show ? View.VISIBLE : View.GONE);
                    diagnosticToggle.setText(show ? "隱藏診斷" : "顯示診斷");
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
        String previous = clipboardStatus.getText().toString();
        String code = MainActivity.extractSixDigitCode(readClipboardText());
        if (code == null) {
            clipboardStatus.setText("剪貼簿：未偵測到 6 位數");
            clipboardHint.setText("先複製代碼，再按下前往剪貼簿代碼。");
        } else {
            clipboardStatus.setText("剪貼簿：" + code);
            clipboardHint.setText("已準備好開啟 " + MainActivity.buildTargetUrl(UrlStore.getBaseUrl(this), code));
        }
        if (previous.length() > 0 && !previous.equals(clipboardStatus.getText().toString())) {
            animateStatusChange();
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
        Drawable shape = roundedBg(bgColor, 12, borderColor, 1);
        int rippleColor = bgColor == Color.WHITE
                ? Color.argb(28, 25, 31, 42)
                : Color.argb(48, 255, 255, 255);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(rippleColor), shape, null));
        attachPressMotion(button);
        return button;
    }

    private void registerEntrance(View view) {
        entranceViews.add(view);
        if (motionEnabled) {
            view.setAlpha(0f);
            view.setTranslationY(dp(12));
        }
    }

    private void playScreenEntrance(final View root) {
        if (!motionEnabled) {
            return;
        }
        entranceRoot = root;
        entranceRunnable = new Runnable() {
            @Override
            public void run() {
                entranceRoot = null;
                entranceRunnable = null;
                for (int i = 0; i < entranceViews.size(); i++) {
                    View view = entranceViews.get(i);
                    view.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setStartDelay(i * 34L)
                            .setDuration(MOTION_NORMAL_MS)
                            .setInterpolator(motionInterpolator)
                            .start();
                }
            }
        };
        root.post(entranceRunnable);
    }

    private void attachPressMotion(final View view) {
        if (!motionEnabled) {
            return;
        }
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View touched, MotionEvent event) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    animateScale(touched, 0.975f);
                } else if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL
                        || action == MotionEvent.ACTION_OUTSIDE) {
                    animateScale(touched, 1f);
                }
                return false;
            }
        });
    }

    private void resetScaleAnimations(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setScaleX(1f);
        view.setScaleY(1f);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                resetScaleAnimations(group.getChildAt(i));
            }
        }
    }

    private void animateScale(View view, float scale) {
        view.animate().cancel();
        view.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(MOTION_FAST_MS)
                .setInterpolator(motionInterpolator)
                .start();
    }

    private void animateStatusChange() {
        if (!motionEnabled) {
            return;
        }
        clipboardStatus.animate().cancel();
        clipboardStatus.setAlpha(0.55f);
        clipboardStatus.setScaleX(0.98f);
        clipboardStatus.setScaleY(0.98f);
        clipboardStatus.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180L)
                .setInterpolator(motionInterpolator)
                .start();
    }

    private void beginContentTransition(ViewGroup container) {
        if (!motionEnabled || !container.isLaidOut()) {
            return;
        }
        Transition transition = new AutoTransition();
        transition.setDuration(220L);
        transition.setInterpolator(motionInterpolator);
        TransitionManager.beginDelayedTransition(container, transition);
    }

    private String historyFingerprint(List<HistoryEntry> entries) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            HistoryEntry entry = entries.get(i);
            builder.append(entry.code).append('\u0001')
                    .append(entry.title).append('\u0001')
                    .append(entry.url).append('\u0001')
                    .append(entry.diagnostic).append('\u0001')
                    .append(entry.timestamp).append('\u0002');
        }
        return builder.toString();
    }

    private boolean shouldUseMotion() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && powerManager.isPowerSaveMode()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ValueAnimator.areAnimatorsEnabled();
        }
        return Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    private void configureWindowForAndroid16() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            View decor = window.getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility()
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setFrameRatePowerSavingsBalanced(true);
            window.setFrameRateBoostOnTouchEnabled(true);
        }
    }

    private void applySystemInsets(final View root) {
        root.setPadding(dp(16), dp(18), dp(16), dp(20));
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                int left;
                int top;
                int right;
                int bottom;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                            | WindowInsets.Type.displayCutout());
                    Insets ime = insets.getInsets(WindowInsets.Type.ime());
                    left = Math.max(bars.left, ime.left);
                    top = Math.max(bars.top, ime.top);
                    right = Math.max(bars.right, ime.right);
                    bottom = Math.max(bars.bottom, ime.bottom);
                } else {
                    left = insets.getSystemWindowInsetLeft();
                    top = insets.getSystemWindowInsetTop();
                    right = insets.getSystemWindowInsetRight();
                    bottom = insets.getSystemWindowInsetBottom();
                }

                view.setPadding(dp(16) + left, dp(18) + top,
                        dp(16) + right, dp(20) + bottom);
                return insets;
            }
        });
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

    private boolean shouldStackHistoryActions() {
        return getResources().getConfiguration().fontScale > 1.15f
                || getResources().getConfiguration().screenWidthDp < 390;
    }

    private LinearLayout.LayoutParams historyActionParams(boolean stacked, int spacing) {
        if (!stacked) {
            return weightedButtonParams(spacing);
        }
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = spacing;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

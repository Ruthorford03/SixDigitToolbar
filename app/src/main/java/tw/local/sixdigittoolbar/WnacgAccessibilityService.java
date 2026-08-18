package tw.local.sixdigittoolbar;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

public class WnacgAccessibilityService extends AccessibilityService {
    private static final int MAX_ATTEMPTS_PER_TEXT = 8;
    private static final int MAX_NODES_PER_SCAN = 300;
    private static final long MIN_ATTEMPT_INTERVAL_MS = 1500;
    private static final long APP_STARTUP_GRACE_MS = 2500;

    private long lastAttemptTime;
    private String lastPendingText = "";
    private int attemptsForPendingText;
    private boolean searchRequested;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable delayedAttempt = new Runnable() {
        @Override
        public void run() {
            attemptPendingPaste();
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event == null ? null : event.getPackageName();
        if (!isWnacgPackage(packageName)) {
            return;
        }

        attemptPendingPaste();
    }

    private void attemptPendingPaste() {
        String pending = WnacgPasteStore.getPending(this);
        if (!HistoryStore.isUsefulTitle(pending)) {
            cancelScheduledAttempt();
            resetAttempts();
            return;
        }

        long startupRemaining = APP_STARTUP_GRACE_MS
                - WnacgPasteStore.getPendingAgeMillis(this);
        if (startupRemaining > 0) {
            scheduleAttempt(startupRemaining);
            return;
        }

        long now = SystemClock.elapsedRealtime();
        long intervalRemaining = MIN_ATTEMPT_INTERVAL_MS - (now - lastAttemptTime);
        if (intervalRemaining > 0) {
            scheduleAttempt(intervalRemaining);
            return;
        }
        lastAttemptTime = now;

        if (!pending.equals(lastPendingText)) {
            lastPendingText = pending;
            attemptsForPendingText = 0;
            searchRequested = false;
        }
        attemptsForPendingText++;
        if (attemptsForPendingText > MAX_ATTEMPTS_PER_TEXT) {
            WnacgPasteStore.clear(this);
            cancelScheduledAttempt();
            resetAttempts();
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            scheduleAttempt(MIN_ATTEMPT_INTERVAL_MS);
            return;
        }
        if (!isWnacgPackage(root.getPackageName())) {
            cancelScheduledAttempt();
            return;
        }

        if (pasteIntoSearch(root, pending)) {
            WnacgPasteStore.clear(this);
            cancelScheduledAttempt();
            resetAttempts();
        } else {
            scheduleAttempt(MIN_ATTEMPT_INTERVAL_MS);
        }
    }

    @Override
    public void onInterrupt() {
        cancelScheduledAttempt();
    }

    @Override
    public void onDestroy() {
        cancelScheduledAttempt();
        super.onDestroy();
    }

    private boolean pasteIntoSearch(AccessibilityNodeInfo root, String text) {
        AccessibilityNodeInfo editable = findBestEditable(root, searchRequested);
        if (editable == null && !searchRequested) {
            AccessibilityNodeInfo search = findSearchLikeNode(root);
            if (search != null && search.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                searchRequested = true;
            }
            return false;
        }

        if (editable == null) {
            return false;
        }

        editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    private AccessibilityNodeInfo findBestEditable(AccessibilityNodeInfo root, boolean allowFallback) {
        ArrayList<AccessibilityNodeInfo> queue = new ArrayList<AccessibilityNodeInfo>();
        queue.add(root);
        AccessibilityNodeInfo fallback = null;

        for (int i = 0; i < queue.size() && i < MAX_NODES_PER_SCAN; i++) {
            AccessibilityNodeInfo node = queue.get(i);
            CharSequence className = node.getClassName();
            boolean editText = className != null
                    && className.toString().toLowerCase().contains("edittext");
            if (node.isEditable() || editText) {
                if (looksSearchRelated(node)) {
                    return node;
                }
                if (allowFallback && fallback == null) {
                    fallback = node;
                }
            }
            for (int childIndex = 0; childIndex < node.getChildCount()
                    && queue.size() < MAX_NODES_PER_SCAN; childIndex++) {
                AccessibilityNodeInfo child = node.getChild(childIndex);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return fallback;
    }

    private AccessibilityNodeInfo findSearchLikeNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }

        ArrayList<AccessibilityNodeInfo> queue = new ArrayList<AccessibilityNodeInfo>();
        queue.add(node);
        for (int i = 0; i < queue.size() && i < MAX_NODES_PER_SCAN; i++) {
            AccessibilityNodeInfo candidate = queue.get(i);
            if (looksSearchRelated(candidate) && candidate.isClickable()) {
                return candidate;
            }
            for (int childIndex = 0; childIndex < candidate.getChildCount()
                    && queue.size() < MAX_NODES_PER_SCAN; childIndex++) {
                AccessibilityNodeInfo child = candidate.getChild(childIndex);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return null;
    }

    private boolean looksSearchRelated(AccessibilityNodeInfo node) {
        String text = toText(node.getText()) + " " + toText(node.getContentDescription()) + " " + toText(node.getViewIdResourceName());
        text = text.toLowerCase();
        return text.contains("搜尋")
                || text.contains("搜索")
                || text.contains("search")
                || text.contains("作品名")
                || text.contains("作者")
                || text.contains("漫畫");
    }

    private String toText(CharSequence text) {
        return text == null ? "" : text.toString();
    }

    private boolean isWnacgPackage(CharSequence packageName) {
        return packageName != null
                && packageName.toString().toLowerCase().contains("wnacg");
    }

    private void scheduleAttempt(long delayMillis) {
        handler.removeCallbacks(delayedAttempt);
        handler.postDelayed(delayedAttempt, Math.max(100L, delayMillis));
    }

    private void cancelScheduledAttempt() {
        handler.removeCallbacks(delayedAttempt);
    }

    private void resetAttempts() {
        lastPendingText = "";
        attemptsForPendingText = 0;
        searchRequested = false;
    }
}

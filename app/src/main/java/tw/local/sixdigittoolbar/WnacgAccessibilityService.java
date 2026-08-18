package tw.local.sixdigittoolbar;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

public class WnacgAccessibilityService extends AccessibilityService {
    private static final int MAX_ATTEMPTS_PER_TEXT = 8;
    private static final int MAX_NODES_PER_SCAN = 300;
    private static final long MIN_ATTEMPT_INTERVAL_MS = 1500;

    private long lastAttemptTime;
    private String lastPendingText = "";
    private int attemptsForPendingText;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event == null ? null : event.getPackageName();
        if (packageName == null || !packageName.toString().toLowerCase().contains("wnacg")) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastAttemptTime < MIN_ATTEMPT_INTERVAL_MS) {
            return;
        }
        lastAttemptTime = now;

        String pending = WnacgPasteStore.getPending(this);
        if (!HistoryStore.isUsefulTitle(pending)) {
            resetAttempts();
            return;
        }

        if (!pending.equals(lastPendingText)) {
            lastPendingText = pending;
            attemptsForPendingText = 0;
        }
        attemptsForPendingText++;
        if (attemptsForPendingText > MAX_ATTEMPTS_PER_TEXT) {
            WnacgPasteStore.clear(this);
            resetAttempts();
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        if (pasteIntoSearch(root, pending)) {
            WnacgPasteStore.clear(this);
            resetAttempts();
        }
    }

    @Override
    public void onInterrupt() {
    }

    private boolean pasteIntoSearch(AccessibilityNodeInfo root, String text) {
        AccessibilityNodeInfo editable = findBestEditable(root);
        if (editable == null) {
            AccessibilityNodeInfo search = findSearchLikeNode(root);
            if (search != null) {
                search.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                editable = findBestEditable(getRootInActiveWindow());
            }
        }

        if (editable == null) {
            return false;
        }

        editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    private AccessibilityNodeInfo findBestEditable(AccessibilityNodeInfo root) {
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
                if (fallback == null) {
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

    private void resetAttempts() {
        lastPendingText = "";
        attemptsForPendingText = 0;
    }
}

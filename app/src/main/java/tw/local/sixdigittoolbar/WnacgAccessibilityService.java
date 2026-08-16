package tw.local.sixdigittoolbar;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class WnacgAccessibilityService extends AccessibilityService {
    private long lastAttemptTime;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pending = WnacgPasteStore.getPending(this);
        if (!HistoryStore.isUsefulTitle(pending)) {
            return;
        }

        CharSequence packageName = event == null ? null : event.getPackageName();
        if (packageName != null && !packageName.toString().toLowerCase().contains("wnacg")) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastAttemptTime < 600) {
            return;
        }
        lastAttemptTime = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        if (pasteIntoSearch(root, pending)) {
            WnacgPasteStore.clear(this);
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
        ArrayList<AccessibilityNodeInfo> candidates = new ArrayList<AccessibilityNodeInfo>();
        collectEditable(root, candidates);
        if (candidates.size() == 0) {
            return null;
        }

        AccessibilityNodeInfo best = candidates.get(0);
        for (int i = 0; i < candidates.size(); i++) {
            AccessibilityNodeInfo candidate = candidates.get(i);
            if (looksSearchRelated(candidate)) {
                return candidate;
            }
        }
        return best;
    }

    private void collectEditable(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out) {
        if (node == null) {
            return;
        }
        CharSequence className = node.getClassName();
        boolean editText = className != null && className.toString().toLowerCase().contains("edittext");
        if (node.isEditable() || editText) {
            out.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectEditable(node.getChild(i), out);
        }
    }

    private AccessibilityNodeInfo findSearchLikeNode(AccessibilityNodeInfo node) {
        if (node == null) {
            return null;
        }
        if (looksSearchRelated(node) && node.isClickable()) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findSearchLikeNode(node.getChild(i));
            if (found != null) {
                return found;
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
}

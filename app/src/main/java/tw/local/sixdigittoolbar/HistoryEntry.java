package tw.local.sixdigittoolbar;

final class HistoryEntry {
    final String code;
    final String title;
    final String url;
    final String diagnostic;
    final long timestamp;

    HistoryEntry(String code, String title, String url, long timestamp) {
        this(code, title, url, "", timestamp);
    }

    HistoryEntry(String code, String title, String url, String diagnostic, long timestamp) {
        this.code = code;
        this.title = title;
        this.url = url;
        this.diagnostic = diagnostic == null ? "" : diagnostic;
        this.timestamp = timestamp;
    }
}

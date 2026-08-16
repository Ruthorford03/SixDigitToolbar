package tw.local.sixdigittoolbar;

final class HistoryEntry {
    final String code;
    final String title;
    final String url;
    final long timestamp;

    HistoryEntry(String code, String title, String url, long timestamp) {
        this.code = code;
        this.title = title;
        this.url = url;
        this.timestamp = timestamp;
    }
}

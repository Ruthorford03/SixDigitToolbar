# Changelog

## 2.9

- 新增首頁「手動輸入」6 位代碼彈窗，並支援點擊剪貼簿狀態直接手動輸入。
- 新增查詢紀錄即時搜尋過濾功能，輸入代碼或作品關鍵字即時篩選。
- 新增查詢紀錄單筆「刪除」功能，並在「清除」按鈕增加防呆確認對話框。
- 修正選取文字同時包含代碼與標題時未開啟瀏覽器的問題，並直接儲存標題。
- 修正重複查詢代碼時時間戳未刷新置頂的邏輯。
- 維護無障礙貼上助手在 wnacg 上的完整自動聚焦與點擊填入相容性。
- 加強標題過濾，排除 Cloudflare、403、Just a moment 等假標題。
- 支援自訂網址主機的 API 端點請求，並升級連線 User-Agent 與 Accept 標頭。
- 升級 Java 相容性至 1.8 並配置 release 自動簽章。

## 2.8

- Show the installed app version beside the app name.
- Collapse history actions by default and reveal one record's actions when its card is tapped.
- Limit titles to two lines in the collapsed state and add a familiar expansion indicator.
- Retain up to 200 records while initially rendering only 20, with 20-record incremental loading.

## 2.7

- Schedule bounded paste retries after the wnacg startup grace period instead of relying on new accessibility events.
- Retry after opening the search screen so the title is filled even when wnacg emits no follow-up event.
- Stop scheduled work immediately after success, timeout, interruption, or leaving the wnacg window.

## 2.6

- Delay the wnacg paste helper until the app startup cover has had time to finish.
- Require a search-labelled control before allowing fallback editable fields.
- Wait for the next accessibility event after opening search instead of acting on a stale screen tree.

## 2.5

- Replace full-layout transitions with short alpha feedback to prevent blank expansion frames.
- Animate the screen as one layer and stop traversing the full view tree when the app leaves the foreground.
- Keep history data cached in-process and reuse the date formatter to reduce repeated main-thread work.
- Fit all history actions on one compact row with stable 44 dp touch targets.
- Back off automatic title fetching for 30 minutes after repeated HTTP 403 or 429 responses.
- Cache pending wnacg paste text and lower accessibility scan frequency and limits.

## 2.4

- Remove card and button elevation shadows; retain clean one-pixel borders and ripple feedback.
- Add a persistent dark-mode switch with a complete dark palette.
- Follow the system dark setting until the user makes an explicit choice.
- Explicitly synchronize OriginOS status and navigation bar icon colors with the selected theme.

## 2.3

- Optimize edge-to-edge insets for Android 16 gesture navigation and display cutouts.
- Enable predictive back and transparent system bars.
- Enable balanced adaptive refresh rate behavior on Android 15 and newer.
- Bound wnacg accessibility scans and ignore unrelated app events earlier.
- Keep the interface stable under OriginOS force-dark and large system font settings.

## 2.2

- Added restrained Apple-style entrance, press, expand, and content-update motion.
- Added native ripple feedback without continuous animation or background work.
- Respect Android animation-disable and battery-saver settings.
- Avoid rebuilding query history when its content has not changed.

## 2.1

- Modernize the main screen with grouped cards, clearer primary actions, and compact history entries.
- Keep the clipboard-first workflow as the default path.
- Move clear history to the history header and keep diagnostics collapsed.

## 2.0

- Avoid title network fetches when a useful title is already cached.
- Throttle repeated title fetch failures and in-progress fetches.
- Reduce title fetch timeouts and response read limits.
- Avoid initial duplicate history rendering.
- Limit Accessibility paste-helper retries and lower event frequency.

## 1.9

- Keep the clipboard-first flow and make it the primary screen action.
- Show detected clipboard code near the top of the screen.
- Collapse URL settings behind a settings button.
- Collapse long title-fetch diagnostics behind a per-entry detail button.

## 1.8

- Use nhentai API v2 gallery endpoint before falling back to the legacy API and HTML.
- Parse title objects from several possible v2 JSON wrapper shapes.

## 1.7

- Store title-fetch diagnostics on failed history items.
- Show HTTP status, exception type, and response snippets for nhentai title fetch failures.

## 1.6

- Add an optional Accessibility paste helper for wnacg search fields.
- Launch wnacg directly and let the paste helper fill the search field when enabled.
- Add a settings shortcut for enabling the paste helper.

## 1.5

- Allow selected/shared title text to update an existing failed history item.
- Add clipboard title update actions for failed automatic title fetches.
- Parse likely Japanese title lines from shared or copied page text.

## 1.4

- Store the Japanese title from nhentai gallery metadata.
- Fall back to the page's Japanese title block before using the browser title.

## 1.3

- Use a stable signing key for future updates.
- Record history before opening the browser.
- Refresh history whenever the settings screen resumes.
- Add a fallback title-fetch thread if Android rejects service startup.

## 1.2

- Add query history for nhentai codes.
- Fetch and store page titles in the background.
- Add history actions to reopen nhentai entries.
- Add wnacg helper action that copies the title and attempts to open/share to wnacg.

## 1.1

- Rename app to nhentai快捷.
- Rename selected-text action to 前往nhentai.
- Use the provided PNG as the launcher icon.
- Add Circle to Search clipboard fallback.

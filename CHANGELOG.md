# Changelog

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

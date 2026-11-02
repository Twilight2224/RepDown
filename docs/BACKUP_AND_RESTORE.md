# Backup and restore

Use **Settings → Export backup**. A backup ZIP contains `data.json` and `manifest.json`; the manifest records format version and SHA-256 checksum. It preserves cards, folders, review schedules and review history, including Unicode text and trashed cards.

Import currently validates the complete ZIP and checksum before a transaction merges records by stable ID. Invalid files are rejected before records are written. Keep the old backup until you verify the imported data. Merge does not erase existing records; a destructive replace mode is intentionally not exposed yet.

For laptop editing use the browser editor’s CSV download. CSV is a simple interchange format and does not preserve review history, tags, or nested-folder relationships; use ZIP for a complete backup.

## Laptop transfer

Use **Settings → Export CSV** to create an editable vocabulary file. The CSV contains stable card IDs and schedule metadata. Edit it in a spreadsheet or in the standalone `RepDawn.html` laptop editor, then use **Settings → Import CSV** on Android. Import merges rows; it does not delete cards missing from the file. Use the ZIP backup for complete native-app backups, including review logs.

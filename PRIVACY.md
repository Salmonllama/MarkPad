# Privacy Policy for MarkPad

**Effective Date:** October 18, 2026  
**Application:** MarkPad (`io.salmonllama.MarkPad`)

---

## Overview

**MarkPad does not collect, store, transmit, share, or sell any personal data, usage data, or telemetry whatsoever.**

MarkPad is a strictly offline-first Markdown scratchpad and note manager for Android. Everything you write, edit, configure, or export stays entirely on your device or within the storage locations you explicitly choose.

---

## 1. Information We Collect

**None.** MarkPad does not collect any information from its users. Specifically:

- **No Personal Information:** We do not ask for, access, or collect your name, email address, phone number, location, contacts, or device identifiers.
- **No User Content Access:** Your notes, scratchpads, checklists, and exported documents are never transmitted to us or any external servers.
- **No Analytics or Telemetry:** MarkPad contains zero analytics SDKs, crash-reporting trackers, usage monitors, or behavioral profiling tools.
- **No Advertisements:** MarkPad contains no advertising SDKs and does not share data with advertisers or data brokers.
- **No Network Access:** MarkPad does not request the `android.permission.INTERNET` permission in its application manifest and cannot transmit data over the internet.

---

## 2. How Your Data Is Stored and Processed

### Local App Storage
All internal scratchpads, pinned note states, and application preferences (such as your selected color theme, editor font, UI font, and cursor positions) are stored locally in MarkPad's private application sandbox on your device using Android's standard SQLite (Room) database and `SharedPreferences`.

### External Files & Storage Access Framework (SAF)
MarkPad allows you to open, edit, live-link, and export `.md`, `.txt`, `.pdf`, and `.docx` files located on your device, in a shared Linux environment, or via third-party document providers (such as Google Drive):
- File access is handled exclusively through the **Android Storage Access Framework (SAF)** system file picker.
- MarkPad can only read or write to the specific individual files that you explicitly select and grant access to.
- If you open or save a file hosted on a cloud provider (such as Google Drive) through Android's system document picker, the synchronization of that file is handled directly by the Android operating system and your chosen cloud provider under their respective privacy policies. MarkPad itself never communicates with any remote servers.

### Android System Backups
MarkPad enables standard Android system auto-backup (`android:allowBackup="true"`) so your internal notes and preferences can be restored if you switch or reset devices. Whether backups occur—and how they are stored—is governed entirely by your device's operating system settings and your platform backup provider (e.g., Google Backup).

---

## 3. Data Sharing and Third Parties

Because MarkPad collects no data, **no data is ever shared with, disclosed to, or sold to any third parties.** The application does not integrate any third-party tracking, analytics, or cloud backend services.

---

## 4. Data Retention and Deletion

You have complete control over all data stored by MarkPad on your device:
- **Deleting Notes:** Deleting an internal note inside MarkPad permanently removes it from the local database.
- **Unlinking External Files:** Closing/unlinking an external file removes the link from MarkPad's sidebar while leaving your original file on disk intact.
- **Uninstalling the App:** Uninstalling MarkPad or clearing its application data in Android Settings permanently erases all internal scratchpads and preferences stored in the app's private storage.

---

## 5. Children's Privacy

MarkPad does not knowingly or unknowingly collect any personal information from anyone, including children under the age of 13. The application complies with the Children's Online Privacy Protection Act (COPPA) and similar international regulations regarding children's privacy.

---

## 6. Changes to This Privacy Policy

If MarkPad's features change in the future in a way that affects this Privacy Policy, any updates will be posted in this document (`PRIVACY.md`) in the project repository with an updated **Effective Date**.

---

## 7. Contact Us

If you have any questions or concerns about this Privacy Policy or MarkPad's privacy practices, please open an issue on the project's GitHub repository:

- **Repository:** [https://github.com/Salmonllama/MarkPad](https://github.com/Salmonllama/MarkPad)

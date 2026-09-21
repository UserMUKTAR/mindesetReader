# MindsetReader — Bug Tracking & Defect History

This document records confirmed bugs encountered during the development of **MindsetReader**, organized by current status: **CURRENT**, **DEFERRED**, and **FIXED**.

---

## 1. CURRENT (Active Issues)

### [BUG-001] Dual-Write Inconsistency Risk (SharedPreferences vs Room)
- **Status**: Open (Technical Debt / Post-Phase 2 Cleanup)
- **Severity**: Low / Technical Debt
- **Description**: `MainActivity.onPageChange` writes reading progress to both SharedPreferences (`preferences.edit()`) and Room (`repository.updateProgress()`). `LibraryActivity` also writes imported book metadata to SharedPreferences via `saveBook()`.
- **Risk**: As long as legacy SharedPreferences writes persist, there is a risk of data drift if one storage system fails or if legacy reads are removed prematurely.
- **Target Resolution**: Complete Phase 2.7–2.8 by removing legacy SharedPreferences writes and transitioning all reads exclusively to Room and DataStore.

### [BUG-002] Unwired Reader Bookmark Button
- **Status**: Open
- **Severity**: Trivial / Incomplete Feature
- **Description**: `bookmarkButton` is declared in `activity_main.xml` and displayed in the bottom-right corner of the PDF reader screen, but has no `setOnClickListener` attached in `MainActivity.kt`. Tapping the button produces no action.
- **Target Resolution**: Wire `bookmarkButton` to persist custom bookmarked pages to Room in Phase 3.

---

## 2. DEFERRED

### [BUG-003] Redundant `MainActivity` Instance in Activity Task Stack
- **Status**: Deferred
- **Severity**: Minor
- **Description**: Neither `MainActivity` nor `LibraryActivity` specifies a `launchMode` in `AndroidManifest.xml` (defaulting to `standard`). When a user navigates Home $\rightarrow$ Library $\rightarrow$ Book, `LibraryActivity` launches `MainActivity` with a new `Intent`, resulting in a task stack of `[MainActivity_1, LibraryActivity_1, MainActivity_2]`.
- **Impact**: When the user presses Back from reader, `MainActivity_2` calls `finish()` and correctly returns to `LibraryActivity_1`. However, if the user opens from Home Continue Reading in `MainActivity_2`, pressing Back closes the activity and returns to `LibraryActivity_1` rather than exiting to Android Home.
- **Proposed Resolution**: Configure `launchMode="singleTop"` or pass `FLAG_ACTIVITY_CLEAR_TOP` when launching `MainActivity` from `LibraryActivity`.

---

## 3. FIXED

### [BUG-004] Wrong Book Opened from Continue Reading / DataStore Race Condition
- **Status**: Verified & Closed (Validated in Phase 2 regression suite)
- **Date Fixed**: Phase 2.6
- **Root Cause**: In `MainActivity.onCreate()`, when launched with `open_pdf = true`, the code previously executed `continueButton.performClick()`. The click listener read `currentLastOpenedUri`, which was populated asynchronously from DataStore (`preferencesRepository.lastOpenedBookUri.collect`). If the user opened Book B from Library, a stale or initial DataStore emission could clobber `currentLastOpenedUri` with Book A, causing Book A to open instead of Book B.
- **Resolution**:
  1. Extracted dedicated `openBook(uriToOpen: String)` method.
  2. Bypassed `continueButton` entirely when `open_pdf = true` and `incomingPdfUri != null`, passing the incoming URI directly to `openBook(incomingPdfUri)`.
  3. Added an emission guard in `lastOpenedBookUri.collect`: if `incomingPdfUri` is present, DataStore emissions with different URIs are discarded.

### [BUG-005] Lost Reading Progress on Back Navigation from Reader
- **Status**: Verified & Closed (Validated in Phase 2 regression suite)
- **Date Fixed**: Phase 2.6
- **Root Cause**: `onPageChange` triggered Room database updates asynchronously via `lifecycleScope.launch { repository.updateProgress(...) }`. When the user tapped Back, `finish()` was invoked immediately. Because the coroutine was not awaited, the activity was destroyed before SQLite completed the `UPDATE` query on `Dispatchers.IO`, dropping the latest page progress.
- **Resolution**: Assigned the coroutine to `lastProgressJob: Job?`. In `onBackPressedDispatcher`, added `lastProgressJob?.join()` before calling `finish()`, guaranteeing SQLite commits before `LibraryActivity` resumes.

### [BUG-006] First-Open of Newly Imported PDF Showing Home Screen
- **Status**: Verified & Closed (Validated in Phase 2 regression suite)
- **Date Fixed**: Phase 2.6
- **Root Cause**: Prior to the navigation decoupling fix, `MainActivity` ran `if (openPdf || isReaderOpen) continueButton.performClick()`. On the very first open of a new book, DataStore had not yet emitted a non-null `lastOpenedBookUri`. Inside `continueButton.setOnClickListener`, the guard `val uriToOpen = currentLastOpenedUri ?: return@setOnClickListener` aborted execution immediately. `openBook()` was never called, leaving `MainActivity` on `homeLayout`.
- **Resolution**: Routed `open_pdf = true` directly to `openBook(incomingPdfUri)`. Verified on emulator with Logcat that no `PDFView.onError` exception occurs and the reader opens immediately.

### [BUG-007] ArithmeticException on Zero-Page Document Progress
- **Commit**: `6993546`
- **Root Cause**: Reading progress calculation `((page + 1) * 100) / pageCount` caused a division-by-zero crash when `pageCount` was `0`.
- **Resolution**: Added safe check: `val progress = if (pageCount > 0) ((page + 1) * 100) / pageCount else 0`.

### [BUG-008] Unhandled SAF Inaccessible File Crash
- **Commit**: `3d86d27`
- **Root Cause**: If an imported PDF file was deleted, renamed, or on inaccessible external storage, `PDFView` threw an unhandled exception crashing the process.
- **Resolution**: Implemented `.onError { ... }` in `PDFView` configurator to catch file access failures, restore Home screen visibility, and display a helpful Toast message.

### [BUG-009] Orphaned Metadata on Book Deletion
- **Commit**: `7052a72`
- **Root Cause**: Deleting a book from `LibraryActivity` removed the UI view but left persistent entries in SharedPreferences and DataStore. If the deleted book was the last opened book, the Continue Reading card remained visible on Home for a non-existent file.
- **Resolution**: Enhanced delete confirmation dialog to remove all SharedPreferences keys (`book_${id}_*`), call `preferencesRepository.clearLastOpenedBookUri()` if the deleted URI matches, and call `repository.deleteBook(id)` in Room.

### [BUG-010] Duplicate Book Entries in Library
- **Commit**: `a99fbae`
- **Root Cause**: Selecting an already imported PDF from the file picker added a duplicate card to the library.
- **Resolution**: Added `repository.isBookDuplicate(bookId)` check before insertion. If a book with the same URI hash already exists, a Toast is shown and insertion is aborted.

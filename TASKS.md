# MindsetReader — Task & Roadmap Tracking

This document tracks the current development roadmap, completed historical phases, active architectural work, and upcoming priorities for the **MindsetReader** project.

---

## 1. COMPLETED

### Phase 0: Project Setup & Prototype
- [x] Initial Android project structure and Gradle build configuration.
- [x] Integrate `AndroidPdfViewer` (`com.github.mhiew:android-pdf-viewer:3.2.0-beta.3`).
- [x] Asset-based PDF loading (`mindset.pdf`) and basic fullscreen reader.
- [x] Basic last-read page saving.

### Phase 1: MVP Features & Core Flow
- [x] **Home Reading Dashboard**:
  - Dynamic daily quotes rotating by day of the year (`quotes.xml`).
  - Continue Reading card displaying last read book title, progress bar, and page indicator.
  - Direct navigation to reader via "Continue Reading" button.
- [x] **Persistent PDF Library (`LibraryActivity`)**:
  - SAF document picker integration (`OpenDocument()`).
  - `takePersistableUriPermission` integration for persistent device storage access.
  - Duplicate book import rejection based on URI hash.
  - Custom list items (`item_pdf_book.xml`) showing progress bars and reading metrics.
  - Four-way library sorting: *Recently Read*, *Recently Added*, *Title*, and *Progress*.
  - Real-time text search filtering books by title.
  - Delete book functionality with AlertDialog confirmation and full metadata cleanup.
- [x] **State & Error Handling**:
  - Reader state preservation across configuration changes / screen rotation via `onSaveInstanceState`.
  - Graceful `.onError` handling for missing or inaccessible files with user toast feedback.
  - Fixed progress division-by-zero when `pageCount == 0`.

### Phase 2: Modern Architecture Migration (Milestones 1–5)
- [x] **Room Database Layer Implementation**:
  - Created `BookEntity` (`@Entity(tableName = "books")`) with full schema: `id`, `uri`, `name`, `last_page`, `page_count`, `progress`, `added_at`, `last_read_at`.
  - Created `BookDao` with reactive `observeAllBooks(): Flow<List<BookEntity>>`, `insertBook`, `updateProgress`, and `deleteBook`.
  - Configured `AppDatabase` singleton Room database builder with KSP compiler.
  - Created `BookRepository` wrapping DAO queries with `Dispatchers.IO`.
- [x] **Legacy SharedPreferences to Room Data Migration**:
  - Created `BookMigrationHelper` to safely migrate legacy `"library"` SharedPreferences entries to Room SQLite database on app startup.
- [x] **LibraryActivity Migration to Room**:
  - Migrated `LibraryActivity` to collect `repository.allBooks` Flow dynamically.
- [x] **Jetpack DataStore Migration**:
  - Added `androidx.datastore:datastore-preferences:1.0.0` dependency.
  - Implemented `AppPreferencesRepository` with cold Flow streams for `last_opened_book_uri` and `library_sort_position`.
  - Implemented `SharedPreferencesMigration` for transparent settings transfer.
  - Migrated `LibraryActivity` sort position read/write to `AppPreferencesRepository`.
- [x] **Critical Navigation Decoupling & Bug Fixes**:
  - Decoupled explicit Library opening from `continueButton.performClick()` in `MainActivity`.
  - Implemented direct `openBook(incomingPdfUri)` invocation when `open_pdf = true`.
  - Guarded DataStore `lastOpenedBookUri.collect` in `MainActivity` against stale emissions overriding explicit incoming book URIs.
  - Implemented coroutine join (`lastProgressJob?.join()`) in `MainActivity.onBackPressedDispatcher` to guarantee Room SQLite writes commit before `LibraryActivity` resumes.
- [x] **Final Phase 2 Regression Suite**:
  - Build & launch: PASS
  - New PDF first-open flow: PASS
  - Existing book progress: PASS
  - Book A/B isolation: PASS
  - Home → Continue Reading: PASS
  - Restart → progress persistence: PASS

---

## 2. IN PROGRESS

### Phase 3: Reader Enhancements & Roadmap Transition
- [ ] **Commit Validated Phase 2 Working Tree**:
  - Stage and commit the validated Phase 2 fixes (`AppPreferencesRepository.kt`, DataStore integration, `openBook` decoupling, and `lastProgressJob?.join()` back-press fix).
- [ ] **Phase 2.7: Deprecate Legacy SharedPreferences Progress Writes**:
  - `MainActivity.onPageChange` currently dual-writes progress to both SharedPreferences (`preferences.edit()`) and Room (`repository.updateProgress`).
  - `LibraryActivity.saveBook()` currently writes imported book metadata to SharedPreferences alongside Room `insertBook()`.
  - *Goal*: Phase out redundant SharedPreferences writes and make Room the single source of truth for all book metadata and reading progress.
- [ ] **Phase 2.8: Unify Home Screen Continue Reading Card**:
  - `MainActivity.updateContinueReadingUI()` still reads `book_${bookId}_name` from SharedPreferences.
  - *Goal*: Update `updateContinueReadingUI()` to fetch title and progress directly from `repository.getBookById(bookId)` or observe Room Flow, removing the last dependency on SharedPreferences.
- [ ] **Phase 3.1: Wire Reader Bookmark Button**:
  - `bookmarkButton` in `activity_main.xml` currently has no click listener attached.
  - *Goal*: Wire `bookmarkButton` to persist custom bookmarked pages in Room.

---

## 3. NEXT (Immediate Next Priorities)

1. **Complete SharedPreferences Phase-Out**:
   - Update `MainActivity` to read Continue Reading data exclusively from Room.
   - Remove legacy `preferences.edit()` calls from `MainActivity.onPageChange`.
   - Remove legacy `saveBook()` calls from `LibraryActivity`.
2. **Implement Functional Page Bookmarking**:
   - Wire `bookmarkButton` to persist custom bookmarked pages in Room or DataStore.
3. **Reading Statistics Dashboard**:
   - Track total reading time, daily streaks, and completion velocity.
4. **Library Card Image / Thumbnail Enhancements**:
   - Extract and cache first-page PDF thumbnails using `PdfiumCore` or `PdfRenderer` for richer library presentation.

---

## 4. DEFERRED / FEATURE PARKING LOT

- **Activity Task Stack Modernization**:
  - Evaluate `launchMode="singleTop"` or `FLAG_ACTIVITY_CLEAR_TOP` for `MainActivity` to avoid stacking multiple `MainActivity` instances when navigating Home $\rightarrow$ Library $\rightarrow$ Reader.
- **Dark Mode & Reader Theme Customization**:
  - Inverted / sepia page rendering filters for comfortable night reading.
- **Reading Statistics Dashboard**:
  - Track total reading time, daily streaks, and completion velocity.
- **Table of Contents (TOC) / Outline Navigation**:
  - Expose PDF document outline bookmarks to allow jumping between chapters.
- **Export / Import Library Backup**:
  - Export user reading history and metadata as a portable JSON backup.

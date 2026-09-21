# MindsetReader — Living Project Documentation

## 1. Project Overview

**MindsetReader** is a focused, distraction-free Android PDF reader and personal reading companion designed to help users build and sustain a daily reading habit. Unlike generic document viewers, MindsetReader centers the user experience around reading progression, daily inspiration, and seamless book management.

### Product Purpose
- Enable readers to maintain a personal library of PDF books stored locally on their device.
- Encourage daily reading with dynamic daily motivational quotes.
- Automatically track and display granular reading progress (page number, total pages, percentage).
- Provide instant one-tap resume to pick up exactly where the reader left off.

### Target Users
- Readers of personal development, mindset, philosophy, and educational books.
- Students and professionals studying technical or long-form PDF books.
- Users who value offline privacy, simplicity, and fast load times over bloated features.

---

## 2. Current Feature Set

### Home Dashboard (`MainActivity`)
- **App Title & Header**: Clean, minimalistic presentation.
- **Daily Reading Quote**: Dynamic motivational quote updated daily using day-of-year rotation from `quotes.xml`.
- **Continue Reading Card**:
  - Displays title of the most recently read book.
  - Horizontal progress bar illustrating overall completion.
  - Progress text displaying percentage and exact page count (e.g., `42% · Page 85 of 200`).
  - **Continue Reading** action button that directly opens the reader to the last read page.
- **Library Navigation**: Direct action button to open the full library.

### In-App PDF Reader (`MainActivity`)
- Built using `com.github.mhiew:android-pdf-viewer:3.2.0-beta.3` (native Pdfium rendering engine).
- Fullscreen reader container toggled dynamically inside `activity_main.xml`.
- Automatic restoration of the user's last read page.
- Real-time progress tracking upon page turns.
- Graceful error handling for missing, moved, or inaccessible files via user-friendly toast alerts.
- State preservation across configuration changes (e.g., screen rotation, theme changes).
- Reader bookmark button interface.

### PDF Library (`LibraryActivity`)
- **Add PDF**: System file picker integration using Android Storage Access Framework (SAF) `ActivityResultContracts.OpenDocument()`.
- **Persistable Permissions**: Uses `contentResolver.takePersistableUriPermission` for persistent document access.
- **Duplicate Prevention**: Rejects duplicate imports based on deterministic URI hash matching.
- **Book Cards**:
  - Displays extracted title (cleaned of `.pdf` extensions and URI encodings).
  - Individual progress bar and formatted page indicators.
  - **Resume** button launching reader directly with targeted URI extras.
  - **Delete** button with confirmation dialog, removing records from SQLite Room, DataStore, and legacy storage.
- **Search**: Live real-time title search filtering library items instantly.
- **Sorting**: Dropdown spinner supporting four persistent sort strategies:
  1. *Recently Read* (descending by `lastReadAt`)
  2. *Recently Added* (descending by `addedAt`)
  3. *Title* (alphabetical A–Z)
  4. *Progress* (descending by completion percentage)

---

## 3. Technology Stack

| Layer | Technology | Version / Notes |
| :--- | :--- | :--- |
| **Language** | Kotlin | 1.9+ (`jvmTarget = "1.8"`) |
| **Platform** | Android SDK | Compile SDK 34, Min SDK 24, Target SDK 34 |
| **UI Framework** | Android Views (XML) | Material Components 1.11.0, ConstraintLayout 2.1.4, AppCompat 1.6.1 |
| **PDF Engine** | AndroidPdfViewer (`mhiew`) | `3.2.0-beta.3` (PdfiumAndroid native wrapper) |
| **Database** | Android Jetpack Room | `2.6.1` with KSP compiler, Room-KTX coroutines & Flow support |
| **Preferences** | Android Jetpack DataStore | Preferences DataStore `1.0.0` with SharedPreferences migration |
| **Concurrency** | Kotlin Coroutines & Flow | `Dispatchers.IO`, `lifecycleScope`, `StateFlow`/`Flow` |
| **Build System** | Gradle Kotlin DSL | Android Application Plugin, Kotlin Android, Google KSP |

---

## 4. Current Architecture

MindsetReader is organized around modern Android architecture principles centered on Room SQLite database and Jetpack DataStore as primary persistence layers. A transitional compatibility layer retains legacy SharedPreferences (`book_*` keys) for backward-compatible metadata and progress reads during the phase-out period:

```
[Presentation Layer]
  MainActivity (Home & PDFView Reader)
  LibraryActivity (Library List, Search, Sort, Import)
         │                           │
         ▼                           ▼
[Repository Layer]
  BookRepository (Singleton)          AppPreferencesRepository (Singleton)
         │                                    │
         ▼                                    ▼
[Storage Layer]
  Room Database (mindset_reader_database)  DataStore (app_preferences)
    - AppDatabase                           - last_opened_book_uri
    - BookDao (Reactive Flow queries)       - library_sort_position
    - BookEntity
```

### Data Layer Components
1. **`BookEntity`** (`data/local/BookEntity.kt`):
   - Table: `books`
   - Primary Key: `id` (String hash of URI)
   - Columns: `id`, `uri`, `name`, `last_page`, `page_count`, `progress`, `added_at`, `last_read_at`.
2. **`BookDao`** (`data/local/BookDao.kt`):
   - `observeAllBooks(): Flow<List<BookEntity>>` (reactive SQLite updates)
   - `getBookById(id: String): BookEntity?`
   - `insertBook(book: BookEntity)`
   - `updateProgress(id, lastPage, pageCount, progress, lastReadAt)`
   - `deleteBook(id)`
3. **`AppDatabase`** (`data/local/AppDatabase.kt`):
   - Singleton Room database instance (`mindset_reader_database`).
4. **`BookRepository`** (`data/BookRepository.kt`):
   - Bridges Room entities to UI model `PdfBook`.
   - Encapsulates database operations on `Dispatchers.IO`.
5. **`AppPreferencesRepository`** (`data/AppPreferencesRepository.kt`):
   - Manages app configuration via Jetpack DataStore Preferences.
   - Manages `last_opened_book_uri` and `library_sort_position`.
   - Includes automatic `SharedPreferencesMigration` from legacy `"library"` preferences.
6. **`BookMigrationHelper`** (`data/local/BookMigrationHelper.kt`):
   - One-time asynchronous migrator reading legacy `"library"` SharedPreferences entries and seeding Room SQLite database on startup.

---

## 5. Development Phases

- **Phase 0: Initial Setup & Single PDF Prototype (Completed)**
  - Basic PDFView display, opening bundled asset PDF, basic last-page memory.
- **Phase 1: Feature Expansion & SharedPreferences MVP (Completed)**
  - Multi-book support, SAF import, persistent library, daily quotes, title search, four-way sorting, delete confirmation, per-book progress tracking.
- **Phase 2: Modern Architecture Migration (Completed & Validated)**
  - Room DB foundation, DAO, Entity, Repository pattern (Completed).
  - Legacy book migration helper (Completed).
  - LibraryActivity migrated to Room Flow (Completed).
  - DataStore preferences migration for sort position and last opened book (Completed).
  - Navigation decoupling: explicit URI opening separated from Continue Reading (Completed).
  - Coroutine write synchronization (`lastProgressJob?.join()`) for reliable back navigation (Completed).
  - **Final Regression Suite**: Passed (Build & launch, new PDF first-open, existing book progress, Book A/B isolation, Home Continue Reading, and restart persistence).
  - Dual-write SharedPreferences `book_*` compatibility layer maintained for existing UI consumers.
- **Phase 3: Reader Enhancements & Bookmarks (Next Phase)**
  - Interactive bookmark management (saving and jumping to custom bookmarks).
  - Reading statistics (daily streaks, total minutes read).
- **Phase 4: Polish & Performance (Planned)**
  - Cover page thumbnail extraction/caching.
  - Night mode reading filter adjustments.

---

## 6. Important Architectural Decisions

1. **Deterministic Book IDs**:
   - `bookId` is computed as `uri.toString().hashCode().toString()`.
   - Ensures stable, deterministic lookup keys across Room, DataStore, and legacy storage without needing auto-incrementing integer IDs.
2. **UID-Scoped Persistable SAF Permissions**:
   - Uses `takePersistableUriPermission` on document import. In Android, persistable permissions belong to the app UID, allowing both `LibraryActivity` and `MainActivity` to read the same stream without intent-level permission grants.
3. **Explicit Intent Navigation vs Continue Reading**:
   - Opening a book from Library launches `MainActivity` with explicit `open_pdf = true` and `pdf_uri = <URI>`.
   - `MainActivity` processes incoming URI directly via `openBook(incomingPdfUri)`, completely decoupled from `continueButton` click handler to prevent mutable DataStore race conditions.
4. **Guaranteed Progress Persistence on Finish**:
   - In `MainActivity`, reader page change updates Room via `lastProgressJob = lifecycleScope.launch { repository.updateProgress(...) }`.
   - When the user presses Back, `onBackPressedDispatcher` explicitly awaits `lastProgressJob?.join()` before calling `finish()`, guaranteeing SQLite commits before `LibraryActivity` resumes.

---

## 7. Scope Boundaries (Non-Goals)

The following items are intentionally out of scope for the current architecture:
- **No Jetpack Compose rewrite**: The app uses traditional Android Views and XML layouts.
- **No Dependency Injection frameworks**: No Dagger or Hilt; singletons use thread-safe `companion object` factories.
- **No Cloud Synchronization / Accounts**: All data and files remain 100% private, on-device, and offline.
- **No Non-PDF Formats**: MindsetReader is specifically optimized for PDF documents (no EPUB, MOBI, or TXT).
- **No In-Document Full-Text Search / Annotation**: PDF text editing, highlighting, and internal text searching are not supported by the current viewer engine.

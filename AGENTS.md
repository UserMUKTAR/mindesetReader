# AI Agent Operating Guidelines — MindsetReader

These guidelines govern how AI coding agents must operate when analyzing, modifying, debugging, or extending the **MindsetReader** codebase. Adherence to these protocols ensures stability, prevents regressions, and maintains high code quality.

---

## 1. Core Principles

1. **Read Project Documentation First**:
   - Always read `PROJECT.md`, `AGENTS.md`, `TASKS.md`, and `BUGS.md` before proposing or making changes.
   - Understand the current architecture, migration phase, and active bug history.
2. **Never Make Broad or Unrequested Changes**:
   - Touch only the files and lines strictly required by the user's prompt.
   - Do not refactor surrounding code, clean up unused imports, rename existing variables, or reformat entire files unless explicitly requested.
3. **Preserve Existing Architecture & Identifiers**:
   - Maintain existing naming conventions (`bookId`, `incomingPdfUri`, `currentLastOpenedUri`, etc.).
   - Respect the dual-persistence state (Room + DataStore with legacy SharedPreferences migration helpers) until explicit migration tasks are assigned.
4. **Investigate Before Fixing**:
   - Never guess or speculate on root causes.
   - Perform a read-only investigation using code tracing, Android SDK semantics, database inspection, and Logcat diagnostics before proposing solutions.
5. **Prefer Minimal, Targeted Edits**:
   - The smallest surgical edit that correctly solves the issue is always superior to a large rewrite.
   - Do not introduce new third-party libraries, DI frameworks, or architectural shifts without explicit user approval.
6. **Build & Verify Compilation**:
   - Always verify that code compiles cleanly after making changes:
     ```powershell
     .\gradlew.bat :app:assembleDebug
     ```
   - If the build fails, immediately diagnose and correct the compilation error before proceeding.
7. **Test Before Declaring Complete**:
   - When an emulator or device is available, verify the behavior using `adb` shell commands, UI hierarchy dumps, or database inspection.
   - A successful Gradle build does not prove runtime correctness.
8. **Git Safety — No Unsolicited Commits or Pushes**:
   - **Never run `git commit` or `git push` unless the user explicitly gives permission in the prompt.**
   - Do not stage unrelated scratch files or build artifacts.

---

## 2. Standard Workflow Protocol

All work on this codebase must follow this sequential lifecycle:

```
┌─────────┐     ┌────────┐     ┌───────────┐     ┌────────┐     ┌────────┐     ┌──────┐
│  SPEC   │ ──> │  PLAN  │ ──> │ IMPLEMENT │ ──> │  TEST  │ ──> │ REVIEW │ ──> │ DONE │
└─────────┘     └────────┘     └───────────┘     └────────┘     └────────┘     └──────┘
```

### Stage 1: SPEC
- Clarify the precise requirements, boundaries, and acceptance criteria from the user prompt.
- Identify all files involved. Check constraints (e.g., "modify ONLY `MainActivity.kt`").
- If the request is ambiguous, ask clarifying questions rather than making assumptions.

### Stage 2: PLAN
- Trace the execution flow across relevant components.
- Check for race conditions, lifecycle pitfalls, and unintended side effects.
- Propose a concise, minimal modification plan to the user before touching code.

### Stage 3: IMPLEMENT
- Make surgical, targeted edits.
- Preserve existing comments, variable names, and code layout.
- Use `replace_file_content` for precise block edits rather than rewriting full files.

### Stage 4: TEST
- Execute `.\gradlew.bat :app:assembleDebug` to confirm build integrity.
- If runtime testing is requested or possible, verify with `adb`:
  - UI state via `uiautomator dump`.
  - Database records via SQLite queries (`run-as com.muktar.mindsetreader`).
  - Logcat diagnostics for crashes or silent exceptions.

### Stage 5: REVIEW
- Compare the exact `git diff` against the original specification.
- Ensure no unexpected files were modified, created, or deleted.
- Verify that no hardcoded strings or broken layouts were introduced.

### Stage 6: DONE
- Summarize the exact changes made, test results, and next steps for the user.
- Await the user's next instruction.

---

## 3. Android-Specific Safeguards for MindsetReader

1. **Storage Access Framework (SAF) & Permissions**:
   - Document URIs obtained via `OpenDocument()` require `takePersistableUriPermission`.
   - Persistable permissions are UID-scoped in Android. Do not add redundant intent flags unless crossing process/package boundaries.
   - Handle `SecurityException` gracefully if cloud providers do not grant persistable rights.
2. **Room Database & Coroutines**:
   - Room updates run asynchronously on `Dispatchers.IO`.
   - Never allow activities to finish while an active database write is in-flight. Use `Job.join()` to guarantee transaction completion.
   - Room `Flow` queries emit on database modification via `InvalidationTracker`, which is asynchronous. Do not assume synchronous in-memory caches match SQLite state immediately upon activity resume.
3. **DataStore Preferences**:
   - DataStore `data` is a cold `Flow`. Guard against stale emissions or racing reads during initial activity setup.
   - Keep app settings (like sort position and last opened URI) in DataStore; keep book domain entities in Room.
4. **Activity Navigation & State**:
   - `MainActivity` serves as both Home Dashboard and PDF Reader via dynamic view swapping (`homeLayout` vs `pdfScreen`).
   - When launching `MainActivity` with explicit `open_pdf = true` and `pdf_uri = ...`, always route directly to `openBook(uri)`—never simulate UI clicks (like `continueButton.performClick()`) that rely on mutable background state.
   - Preserve reader visibility and current URI across configuration changes via `onSaveInstanceState`.

# Testriq — IELTS, SAT & PTE Practice

Testriq is a responsive, installable Progressive Web App for original exam-practice activities. It is designed to be easy to host on GitHub Pages.

## Included
- IELTS Reading and Listening mock-style mini-tests, Speaking interview prompts, Writing practice, grammar and academic vocabulary.
- Additional generated practice sets from `bank.js`, including 16 IELTS speaking topics, original reading passages, listening scripts, grammar/vocabulary and SAT/PTE practice.
- Voice-guided speaking practice: the app reads questions aloud using browser text-to-speech and can transcribe answers using the browser's Web Speech API where supported.
- A clearly labelled, heuristic speaking practice estimate based on transcript text. This is **not an official IELTS band score and is not a validated AI assessment**. It cannot reliably evaluate pronunciation or intonation. A genuine AI assessment requires a secure backend and a speech/LLM provider; never put private API keys in GitHub Pages client-side code.
- Timed multiple-choice practice, explanations, results history, local writing drafts, progress, theme/accessibility settings and data export.
- Custom Testriq logo (`logo.svg` / `icon.svg`), PWA manifest and service worker.

## Run locally
Use a local web server (recommended), or open `index.html` for most non-microphone practice. The service worker and microphone permissions require HTTPS or localhost. Microphone support varies by browser; Chrome on Android is a good starting point. Grant microphone permission when prompted.

## Publish on GitHub Pages
1. Create a public repository, for example `testriq`.
2. Upload **all files** in this folder to the repository root: `index.html`, `bank.js`, `manifest.json`, `sw.js`, `icon.svg`, `logo.svg`, and `README.md`.
3. In the repository, open **Settings → Pages**.
4. Choose **Deploy from a branch**, select `main` and `/ (root)`, then save.
5. Wait for deployment and open the HTTPS URL. Refresh once after the first load so the updated service worker can install.
6. On Android Chrome, open the site menu and choose **Install app** or **Add to Home screen**.

Keep `bank.js` alongside `index.html`; the app loads the extra question bank from it. The service worker caches both files for offline app-shell use. Speech recognition may require an internet connection even when the rest of the app works offline.

## Scoring and privacy
Practice attempts, settings and drafts are saved in browser local storage on that device. There is no account, cloud sync or server-side AI in this release. Browser speech recognition may send audio to the browser vendor's recognition service according to its settings and terms. The app does not store raw audio. The speaking estimate uses text-length and vocabulary-diversity heuristics only, so use it for practice feedback—not admissions, visa, or official score decisions.

## Important
All included practice content is original and is not official IELTS, British Council, IDP, Cambridge, College Board or Pearson material. Testriq is independent and is not affiliated with those organisations. IELTS Speaking prompts are organised in a mock interview style; these are mini-mocks rather than a complete official-length exam.

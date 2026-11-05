# Testriq — IELTS, SAT & PTE practice (website + Android app)

Testriq is an exam-practice app with a live AI speaking examiner, AI writing marking, IELTS daily goals and a weekly band report. The same app runs as:

- **Website**: GitHub Pages serves `index.html` from this repository.
- **Android app**: the `android/` project wraps the same files in a native app and adds a male/female examiner voice, speech recognition, Google/Facebook sign-in, reminders and haptics. It's built automatically into a Google Play bundle (`.aab`).

**Start here → [SETUP.md](SETUP.md)** covers the signing secret, the AI examiner, accounts, publishing on Google Play and updating.

## What's where

| Path | What it is |
| --- | --- |
| `index.html` | The whole app: courses, tests, live speaking examiner, writing, welcome/sign-in, goals, settings |
| `config.js` | **Your settings**: the AI examiner address, Firebase keys and links. Edit this, not the code. |
| `worker.js` | The AI examiner (Cloudflare Worker using Google Gemini): follow-up questions, speaking/writing band scores, weekly report |
| `auth.html` | Google / Facebook sign-in page (opened by the app in a secure browser tab) |
| `privacy.html` | Privacy policy and account-deletion page (required by Google Play) |
| `android/` | Native Android project (Java, no extra libraries). `android/version.properties` holds the version name. |
| `.github/workflows/android.yml` | Builds the signed `.aab` and `.apk` and publishes them under **Releases** |
| `store/` | Play Store icon, feature graphic and phone screenshots |

## The examiners

- **Adib Bhuiyan** speaks with a male voice; **Jishad Mozmder** with a female voice. The Android app picks matching voices from the phone's text-to-speech engine.
- In Part 1 and Part 3 the examiner asks follow-up questions about what you actually said, and sometimes challenges your answer, as in a real test.
- At the end, the AI examiner marks Fluency & Coherence, Lexical Resource, Grammatical Range & Accuracy and Pronunciation. It uses whole bands per criterion and official rounding for the overall band, and it lists corrections taken from your own answers. Without a connection, an on-device estimate is used.

## Notes

All practice content is original and is not official IELTS, British Council, IDP, Cambridge, College Board or Pearson material. Scores are practice estimates, not official results.

Designed and developed by Md Sajid Shahriar Bhuiyan · SYMETREE.

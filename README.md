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
| `worker.js` | The server (Cloudflare Worker): Gemini AI examiner (follow-up questions, speaking/writing band scores, weekly report) and cloud accounts (D1 database) |
| `auth.html` | Google / Facebook sign-in page (opened by the app in a secure browser tab) |
| `privacy.html` | Privacy policy and account-deletion page (required by Google Play) |
| `android/` | Native Android project (Java, no extra libraries). `android/version.properties` holds the version name. |
| `.github/workflows/android.yml` | Builds the signed `.aab` and `.apk` and publishes them under **Releases** |
| `store/` | Play Store icon, feature graphic and phone screenshots |

## The examiners

- **Sajid Shahriar** speaks with a male voice; **Jishad Mozmder** with a female voice. The Android app picks matching voices from the phone's text-to-speech engine.
- In Part 1 and Part 3 the examiner asks follow-up questions about what you actually said, and sometimes challenges your answer, as in a real test.
- The examiner remembers your answers during the test: it links later questions to things you said earlier, points out when an answer contradicts an earlier one, asks again when you don't choose between two options, and adapts work/study questions to what you actually do.
- All 120 IELTS speaking tests (Part 1, Part 2, Part 3 and Full tests) use different questions; none is repeated.
- **Live judge:** during IELTS speaking tests a live band meter updates after every answer, with a quick tip. It scores the four criteria from what you said (answer development, linking, vocabulary range, idioms, grammar range and slips) and how you said it (speaking rate, pauses, response delay, recognition clarity), across the full band range 3–9.
- At the end, the AI examiner (when connected) marks Fluency & Coherence, Lexical Resource, Grammatical Range & Accuracy and Pronunciation. It uses whole bands per criterion and official rounding for the overall band, and it lists corrections taken from your own answers. Without a connection, the live judge's full report is used.

## Version 1.0.2

- **Accounts that survive reinstalling.** Cloud accounts on the Testriq server (Worker + D1, see SETUP.md step 4). Without a server, the Android app keeps the account and progress in the phone's Google backup, restored automatically after reinstalling, and learners can save and restore a backup file.
- **Reminders that arrive.** The daily reminder is on by default (19:00) and works in battery-saving mode. A background check delivers reminders that phone makers' battery savers dropped, and sends friendly "come back" notifications after days away. If notifications were declined, the app asks again later (at most three times) and opens the right settings page. Settings → "Reminders not arriving?" fixes battery restrictions.
- **A real speaking judge.** A new on-device live judge gives different, evidence-based bands from 3 to 9 (the same test no longer gives everyone the same band). A live band meter shows during the test, the report shows corrections from your own words, and "How your band developed during the test".
- **Smarter, more natural examiner.** The AI examiner uses the full band range with clear anchors and gets measured evidence (pauses, response delay, linking, vocabulary, grammar range, likely slips). Follow-ups use natural spoken English with a short acknowledgement. The Android voice prefers Google's natural online voices (with offline fallback) and no longer pitch-shifts real voices.
- **New light theme.** A soft ash-grey, lightly textured background with pearl cards and frosted bars. Plus a friendlier home screen (greeting and streak) and a reminders banner.

## Version 1.0.1

- Examiner 1 is now **Sajid Shahriar** (male voice).
- Grammar & Vocabulary: **50 tests × 10 questions**, every question different, in five difficulty levels (Easy, Medium, Hard, Advanced, Expert) shown on each test.
- Smarter speaking examiner with memory of your answers; every speaking test is different.
- The opening screen is drawn natively, so the app starts without a blank pause and the logo animation stays smooth.

## Notes

All practice content is original and is not official IELTS, British Council, IDP, Cambridge, College Board or Pearson material. Scores are practice estimates, not official results.

Designed and developed by Md Sajid Shahriar Bhuiyan · SYMETREE.

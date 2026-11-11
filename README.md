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

## Version 1.0.3.3

- **Grammar & Vocabulary tests are numbered by level:** Tests 1–20 Easy, 21–40 Medium, 41–60 Hard, 61–80 Advanced and 81–100 Expert.
- The level key at the top (Easy ≈ Band 5 … Expert ≈ Band 9) and the level tag on every test stay as they were. Each level now starts with a heading showing its test numbers, the time per question and how many of its tests you've finished. Tap a level in the key to jump straight to it.
- Results and ticks you already have stay with the same tests; only their numbers change, and your saved results and mistake notebook show the new numbers.

## Version 1.0.3.2

- **10 full-length IELTS mock tests (Mock Tests 21–30).** Exactly like test day: Listening (4 parts, 40 questions, about 30 minutes), Reading (3 passages, 40 questions, 60 minutes), Writing (Task 1 and Task 2, 60 minutes) and Speaking (Parts 1–3) — about 2 hours 45 minutes. All Listening recordings and Reading passages are new and original, with real IELTS question types (form and note completion, multiple choice, matching, matching headings, TRUE/FALSE/NOT GIVEN, YES/NO/NOT GIVEN, sentence endings). Each paper can also be taken on its own from the Full Test folders.
- **100 Grammar & Vocabulary tests** (50 new): 20 at each level — Easy, Medium, Hard, Advanced and Expert — every question different, each with an explanation.
- **Resume or start over.** Opening a test you left part-way now asks whether to resume (with your saved answers) or start over. Works for quizzes, Listening and Reading sections, Writing and mock tests.
- **AI marking is more reliable on slow connections.** The app now waits up to about two minutes (the server can take up to ~85 s when it has to try several AI models) and stops the clock once the reply starts arriving, so a reply is never cut off. A reply that arrives empty or broken is fetched again automatically.
- When AI marking still can't finish, the message says why (busy, daily limit, no connection or a short code), instead of "incomplete report".

## Version 1.0.3.1

- **Completed tests keep their green tick** after closing and reopening the app (before, they only came back after finishing another test).
- **Practice shows every course:** IELTS, SAT and PTE, grouped by section with how many tests you've done; tap a section to open it.
- **Question timer:** each Grammar & Vocabulary question (and every timed question) gets its full time. The next question's clock no longer runs while you read the explanation of the last one.
- Releases are named by version only (for example "Testriq 1.0.3.1"), and Settings shows just the version.

## Version 1.0.3

- **A new look.** One set of hand-drawn line icons across the whole app (they fill in on the selected tab), proper IELTS, SAT and PTE course icons, and the Inter typeface built in, so it looks the same on every phone.
- **A calmer speaking test.** Illustrated examiners (Sajid and Jishad), a sound wave that moves while the examiner speaks and with your voice while you answer, and a live band meter that folds away (tap it for the details).
- **Pronunciation marked from your voice.** In IELTS speaking tests your answers are recorded (Android 13 and newer, the website on a computer, and the Windows app) and up to two minutes are sent to the AI examiner with the transcript, so pronunciation is judged from how you actually sound. It can be switched off in Settings.
- **Band rings and progress charts.** Each criterion in a report has a band ring. Progress shows your band over time for each skill and for each speaking and writing criterion, with a tooltip and a table view.
- **Mistake notebook.** Every correction from your speaking and writing, quiz questions you missed and better words for the plain ones you use are saved automatically. A short daily review (spaced repetition: 1, 2, 4, 8, 16, 32 days) makes them stick; the home screen shows today's cards.
- **Full timed mock exam.** Take a mock test as one exam: Listening, Reading, Writing and Speaking back to back with a one-minute break between sections, then one overall band.
- **Google sign-in and password reset by email** through the Testriq server (SETUP.md steps 5–6), and **fair daily limits** on AI marking for each phone, so the free Gemini quota is shared fairly.
- Friendly empty screens with a clear next step everywhere.

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

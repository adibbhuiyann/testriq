# Testriq — setup, Play Store and updates

Everything below can be done from a phone browser. Do the steps in order.
Steps 1–2 give you the app files. Steps 3–4 switch on the Gemini AI examiner and cloud accounts (one free Cloudflare Worker does both). Step 5 adds password-reset emails and step 6 Google sign-in (both free, optional). Step 7 is the optional Firebase alternative. Step 8 puts the app on Google Play. Step 9 covers updates.

> **Important:** until you finish steps 3 and 4 and put the Worker address in `config.js`, the app runs fully offline: speaking and writing are marked by the on-device live judge, and accounts live on the phone (protected by Android backup and backup files). The app works, but the Gemini examiner and "log in on any phone" need the Worker.

---

## 1. Add the signing password to GitHub (one time, 1 minute)

Your app is signed with a private **upload key**. The key is stored in this repository in encrypted form (`android/keystore/upload-keystore.p12.enc`). Only the password can unlock it, and only GitHub knows it.

1. Open **github.com/adibbhuiyann/testriq → Settings → Secrets and variables → Actions**.
2. Tap **New repository secret**.
3. Name: `ANDROID_KEYSTORE_PASSWORD`
4. Secret: the password from the `testriq-signing-key.txt` file you were given.
5. Tap **Add secret**.

> Keep `testriq-upload.p12` and its password somewhere safe (Google Drive, email to yourself). You need the same key for **every** future update. If you ever lose it, Google Play can reset your upload key (Play Console → Test and release → App integrity), but that takes a few days.

## 2. Build the app

- The build runs automatically every time you change the app files on `main`.
- To run it by hand: **Actions → Build Testriq Android app → Run workflow**.
- When it finishes (about 5 minutes), open **Releases** (right side of the repository's front page) and download:
  - `Testriq-x.y.z-NNN.aab`: upload this to Google Play.
  - `Testriq-x.y.z-NNN.apk`: install this directly on your phone to test.

If a build fails, the error log is saved to the `ci-logs` branch (`last-build.log`).

## 3. Turn on the AI examiner (Cloudflare Worker + Gemini, free)

The AI examiner asks follow-up questions that challenge your answers in Part 1 and Part 3. It also marks speaking and writing against the four IELTS criteria and writes the weekly band report. Without it, the app still works and uses on-device estimates.

1. Get a free Gemini API key at **aistudio.google.com/apikey** → *Create API key*. Copy it.
2. Go to **dash.cloudflare.com** (free account) → **Workers & Pages → Create → Create Worker**. Name it `testriq-ai` → **Deploy**.
3. Tap **Edit code**, delete everything, paste the whole of `worker.js` from this repository, then tap **Deploy**.
4. Go back to the worker → **Settings → Variables and Secrets → Add**:
   - Type **Secret**, name `GEMINI_KEY`, value = your Gemini key → **Deploy**.
5. Copy the worker's address, for example `https://testriq-ai.yourname.workers.dev`.
6. In this repository, open `config.js` → ✏️ edit → put the address in `aiUrl: "..."` → **Commit changes**.
7. Check it works: open the worker address in a browser. It should show `"configured": true`. In the app, go to **Settings → AI examiner → Test**.

The worker automatically uses the newest Gemini Flash model your key can access. If one model is busy or over its free quota, it falls back to the next one.

## 4. Turn on cloud accounts (same Worker, free, 3 minutes)

This makes accounts and progress survive deleting the app, a new phone or a factory reset: the learner just logs in again.

1. **dash.cloudflare.com → Storage & Databases → D1 SQL Database → Create**. Name it `testriq-users` → **Create**. (You don't need to create any tables; the Worker does it.)
2. Open your `testriq-ai` Worker → **Bindings** (or **Settings → Bindings**) → **Add binding → D1 database**.
   - Variable name: `DB`
   - D1 database: `testriq-users` → **Add binding** / **Deploy**.
3. Check it: open the Worker address in a browser. It should now show `"accounts": true`.
4. Make sure `aiUrl` in `config.js` is your Worker address (step 3.6), then rebuild the app.

What learners see: "Create an account" and "Log in with email" now save to the cloud. Anyone who already made an account on their phone gets a **Save online** button in Settings (and is moved online automatically the next time they log in). Passwords are stored only as salted PBKDF2 hashes.

Password reset by email needs an email service: step 5 sets it up for free.

**Fair use (automatic):** with the database bound, the Worker counts AI marks per phone and per internet address each day — 30 speaking marks, 15 voice marks, 25 writing marks and 12 weekly reports per phone, and 3,000 AI marks a day for everyone together. A learner who reaches a limit sees the on-device band and a friendly message. To change the overall cap, add a **Text** variable `DAILY_AI_CAP` (for example `5000`). Requests that don't come from the Testriq app or website are refused.

Without this step, accounts are kept on the phone. They are still protected: the Android app copies them into the phone's Google backup (restored automatically when Testriq is reinstalled on the same Google account), and learners can save a **backup file** in Settings → Data & privacy and restore it from the login screen.

### Automatic server updates (recommended, one time)

`wrangler.jsonc` tells Cloudflare how to deploy `worker.js` (Worker name `testriq`, the `DB` database). Connect it once and every change to `worker.js` on `main` goes live automatically, with no copying and pasting:

1. **Workers & Pages → testriq → Settings → Build → Connect** (Git repository).
2. Choose **GitHub**, allow Cloudflare to access `adibbhuiyann/testriq`, and pick branch **main**.
3. Build command: leave empty. Deploy command: `npx wrangler deploy`. Root directory: `/`.
4. Build watch paths (if offered): `worker.js` and `wrangler.jsonc`, so app-only changes don't redeploy the server.
5. Save. Cloudflare deploys straight away; check the Worker address shows the latest `"version"`.

The Gemini key is not in the repository: it stays a Secret (`GEMINI_KEY`) in the dashboard, and deploys keep it.

## 5. Password reset by email (Brevo, free, 10 minutes)

Learners who forget their password get a 6-digit code by email and choose a new one in the app. Brevo's free plan sends 300 emails a day.

1. Sign up at **brevo.com** (free plan).
2. **Senders, domains & dedicated IPs → Senders → Add a sender**: name `Testriq`, email = an address you can open (for example `symetree4@gmail.com`). Brevo emails you a link — open it to verify the sender.
   (Better delivery later: verify your own domain under **Domains** and use an address on it.)
3. **SMTP & API → API keys → Generate a new API key** → name it `testriq` → copy the key (it starts with `xkeysib-`).
4. In Cloudflare, open the `testriq` Worker → **Settings → Variables and Secrets → Add**:
   - Type **Secret**, name `BREVO_KEY`, value = the Brevo key.
   - Type **Text**, name `MAIL_FROM`, value = the sender email you verified in step 2.
   Tap **Deploy**.
5. Check it: the Worker address should now show `"reset": true`. In the app: **Log in → Forgot password?** → enter your email → you receive a code.

## 6. Google sign-in (free, 10 minutes)

Learners tap **Continue with Google**, choose their Google account, and come straight back to Testriq, signed in. Their account lives on your Testriq server like an email account (so their progress also comes back after reinstalling).

1. Open **console.cloud.google.com** → top bar → **Select a project → New project** → name `Testriq` → **Create**.
2. **APIs & Services → OAuth consent screen** (Google Auth Platform → **Get started**): app name `Testriq`, support email = yours, audience **External**, contact email = yours → **Create**. Under **Branding**, add the app domain `adibbhuiyann.github.io` and the privacy policy link `https://adibbhuiyann.github.io/testriq/privacy.html`. Under **Audience**, tap **Publish app** (sign-in only asks for name, email and photo, so no review is needed).
3. **Clients → Create client** (or **Credentials → Create credentials → OAuth client ID**):
   - Application type: **Web application**, name `Testriq web`.
   - **Authorised JavaScript origins → Add URI**: `https://adibbhuiyann.github.io`
   - **Create**, then copy the **Client ID** (it ends with `.apps.googleusercontent.com`).
4. In this repository, open `config.js` → ✏️ edit → paste it into `googleClientId: "..."` → **Commit changes**.
5. In Cloudflare, open the `testriq` Worker → **Settings → Variables and Secrets → Add** → Type **Text**, name `GOOGLE_CLIENT_ID`, value = the same Client ID → **Deploy**.
6. Check it: the Worker address shows `"google": true`. Rebuild the app (it builds by itself after the commit). The welcome screen now has **Continue with Google**. It opens a secure Chrome tab with Google's button, then returns to the app.

## 7. Optional: Firebase instead of the Testriq server

Not needed if you did steps 4–6. Firebase is an alternative home for accounts, with Facebook sign-in. When Firebase is set up, it is used for accounts instead of the Worker.

1. **console.firebase.google.com → Add project** → name `testriq` → create. You don't need Google Analytics.
2. **Build → Authentication → Get started → Sign-in method**. Enable **Email/Password** and **Google**.
3. **Authentication → Settings → Authorised domains → Add domain**: `adibbhuiyann.github.io`
4. **Build → Firestore Database → Create database** (production mode, any location). Then open **Rules**, replace everything with the rules below and tap **Publish**:

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid} {
         allow read, write, delete: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```
5. **Project settings (⚙) → General → Your apps → Web (</>)** → register the app. Copy `apiKey`, `authDomain`, `projectId` and `appId` into the `firebase` section of `config.js` → commit.

Google sign-in now works on the website and in the app (it opens a secure Chrome tab, then returns to Testriq).

### Facebook sign-in (optional)

1. **developers.facebook.com → My Apps → Create app** → use case **Authenticate and request data from users with Facebook Login**.
2. **App settings → Basic**: add the Privacy Policy URL `https://adibbhuiyann.github.io/testriq/privacy.html` and the User data deletion URL `https://adibbhuiyann.github.io/testriq/privacy.html#delete-account`. Copy the **App ID** and **App secret**.
3. In Firebase: **Authentication → Sign-in method → Add new provider → Facebook**. Paste the App ID and secret. Copy the **OAuth redirect URI** it shows (`https://<project>.firebaseapp.com/__/auth/handler`).
4. In Facebook: **Facebook Login → Settings → Valid OAuth Redirect URIs** → paste that URI → save.
5. Switch the Facebook app to **Live** mode.

To hide a button you don't want, set `googleSignIn` or `facebookSignIn` to `false` in `config.js` (Facebook is off by default: set it to `true` after these steps).

## 8. Publish on Google Play

1. **play.google.com/console** → create a developer account (one-time US$25 fee, identity verification).
2. **Create app**: name *Testriq*, language English, App, Free.
3. **Store listing**: use the files in `store/`. `play-icon-512.png` is the app icon and `play-feature-graphic-1024x500.png` is the feature graphic. The phone screenshots are `screenshot-1…4.png`.
   - Short description (max 80): *IELTS practice with a live AI speaking examiner, band scores and daily goals.*
4. **App content** (left menu → Policy → App content):
   - Privacy policy: `https://adibbhuiyann.github.io/testriq/privacy.html`
   - Ads: **No**. App access: **All functionality is available without special access** (guest mode).
   - Content rating questionnaire: Education/Reference. Target audience: **13+** (not designed for children).
   - Data safety (what the app does):
     - *Personal info → Email address, Name*: collected, optional (only with an account), for **Account management**; encrypted in transit; users can request deletion.
     - *Audio → Voice or sound recordings*: **collected** (IELTS speaking answers are sent to the AI examiner for pronunciation marking), **optional** (Settings → "Mark my pronunciation from my voice"), purpose **App functionality**; not shared (Google Gemini processes it for you as a service provider); encrypted in transit; deleted after marking.
     - *App activity → Other user-generated content* (answers sent for AI marking): collected for **App functionality**; encrypted in transit.
     - *Device or other IDs*: **collected** (a random ID made by the app, used to share the free AI marks fairly), purpose **Fraud prevention, security, and compliance**; not shared.
     - Microphone use is declared automatically from the app's permissions.
     - Account deletion URL: `https://adibbhuiyann.github.io/testriq/privacy.html#delete-account`
5. **Closed test (required for new personal accounts): at least 12 testers, opted in for 14 days in a row.**
   1. **Test and release → Testing → Closed testing → Create track** (or use the default *Alpha* track) → **Create new release** → upload the newest `.aab` from Releases → release name `1.0.3` → **Save → Review → Start rollout**.
   2. **Testers** tab: choose **Google Groups** (easiest: create a free group at groups.google.com, for example `testriq-testers@googlegroups.com`, and invite people) or an **email list** (paste their Gmail addresses). Add at least **15** people so you stay above 12 if someone leaves.
   3. Copy the **opt-in link** on the Testers tab and send it to everyone. Each tester opens it **on their Android phone, signed in with the same Google account you added**, taps **Become a tester**, then installs Testriq from the Play Store link on that page.
   4. Ask testers to keep the app installed, open it every day or two and send feedback (Play lets them leave private feedback). The 14 days count from when 12 testers have opted in — check **Closed testing → Testers**.
   5. Keep uploading fixes to the same closed track during the test; it doesn't reset the 14 days.
   6. After 14 days: **Dashboard → Apply for production** → answer the questions about your test (how you found testers, what feedback you got, what you changed). Google usually replies within about 7 days.
6. When production access is approved: **Production → Create new release** → upload the newest `.aab` → roll out.

Use **Play App Signing** (the default). Google then re-signs the app for users, and your upload key only proves that the update came from you.

## 9. Updating the app later

1. Change the app on GitHub, for example `index.html`, `config.js`, the question banks or `android/…`.
2. Open `android/version.properties` and raise `VERSION_NAME` (for example `1.0.2` → `1.0.3`). This is the version users see.
3. Commit to `main`. GitHub builds a new `.aab` automatically. The version code goes up by itself with every build, which Google Play requires.
4. In Play Console, create a new release on the same track and upload the new `.aab`.
5. If you changed `worker.js` and connected automatic server updates (step 4), Cloudflare deploys it by itself. Otherwise paste it into your Cloudflare Worker (**Edit code → Deploy**), or paste the parts in `worker-parts/` one after another from a phone. The app works with an older Worker too, but new features (voice marking, reset codes, Google sign-in) need the new one.

The website (GitHub Pages) uses the same `index.html`, so it updates at the same time.

> If you installed a test `.apk` from Releases and later install Testriq from Google Play, uninstall the test copy first. The two copies are signed differently.

## Building on a computer instead (optional)

Open the `android/` folder in Android Studio (Ladybug or newer, JDK 17). For a signed build, set these environment variables: `TESTRIQ_KEYSTORE=/path/to/testriq-upload.p12` and `TESTRIQ_KEYSTORE_PASSWORD=...`. Then run `./gradlew bundleRelease`. The web files are copied into the app automatically on every build.

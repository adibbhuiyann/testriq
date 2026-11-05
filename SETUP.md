# Testriq — setup, Play Store and updates

Everything below can be done from a phone browser. Do the steps in order.
Steps 1–2 give you the app files. Steps 3–5 switch on the smart AI examiner and Google/Facebook sign-in. Step 6 puts the app on Google Play. Step 7 covers updates.

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

## 4. Turn on accounts and cloud sync (Firebase, free)

Until this is done, email accounts are saved only on the device, and the Google/Facebook buttons explain that they're "coming soon".

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

## 5. Facebook sign-in (optional)

1. **developers.facebook.com → My Apps → Create app** → use case **Authenticate and request data from users with Facebook Login**.
2. **App settings → Basic**: add the Privacy Policy URL `https://adibbhuiyann.github.io/testriq/privacy.html` and the User data deletion URL `https://adibbhuiyann.github.io/testriq/privacy.html#delete-account`. Copy the **App ID** and **App secret**.
3. In Firebase: **Authentication → Sign-in method → Add new provider → Facebook**. Paste the App ID and secret. Copy the **OAuth redirect URI** it shows (`https://<project>.firebaseapp.com/__/auth/handler`).
4. In Facebook: **Facebook Login → Settings → Valid OAuth Redirect URIs** → paste that URI → save.
5. Switch the Facebook app to **Live** mode.

To hide a button you don't want, set `googleSignIn` or `facebookSignIn` to `false` in `config.js`.

## 6. Publish on Google Play

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
     - *Audio → Voice or sound recordings*: processed on the device by the phone's speech service, not stored by Testriq. Declare it only if the form asks about data that is processed but not stored.
     - *App activity → Other user-generated content* (answers sent for AI marking): collected for **App functionality**; encrypted in transit.
     - Account deletion URL: `https://adibbhuiyann.github.io/testriq/privacy.html#delete-account`
5. **Testing → Closed testing → Create track** → upload the `.aab` → add testers (an email list or a Google Group) → roll out.
   New personal developer accounts must run a closed test with **at least 12 testers who stay opted in for 14 days in a row**. After that, you can **apply for production access**.
6. When production access is approved: **Production → Create new release** → upload the newest `.aab` → roll out.

Use **Play App Signing** (the default). Google then re-signs the app for users, and your upload key only proves that the update came from you.

## 7. Updating the app later

1. Change the app on GitHub, for example `index.html`, `config.js`, the question banks or `android/…`.
2. Open `android/version.properties` and raise `VERSION_NAME` (for example `1.0.0` → `1.0.1`). This is the version users see.
3. Commit to `main`. GitHub builds a new `.aab` automatically. The version code goes up by itself with every build, which Google Play requires.
4. In Play Console, create a new release on the same track and upload the new `.aab`.

The website (GitHub Pages) uses the same `index.html`, so it updates at the same time.

> If you installed a test `.apk` from Releases and later install Testriq from Google Play, uninstall the test copy first. The two copies are signed differently.

## Building on a computer instead (optional)

Open the `android/` folder in Android Studio (Ladybug or newer, JDK 17). For a signed build, set these environment variables: `TESTRIQ_KEYSTORE=/path/to/testriq-upload.p12` and `TESTRIQ_KEYSTORE_PASSWORD=...`. Then run `./gradlew bundleRelease`. The web files are copied into the app automatically on every build.

/* =====================================================================
   Testriq settings — edit this one file to connect your services.
   It is used by the website AND the Android app (rebuild the app after
   changing it). Everything here is safe to be public: no secret keys.
   See SETUP.md for step-by-step instructions.
   ===================================================================== */
window.TESTRIQ_CONFIG = {
  // 1) AI examiner AND cloud accounts — the URL of your Cloudflare Worker (worker.js).
  //    Example: "https://testriq-ai.adibbhuiyann.workers.dev"
  //    While this is empty the app marks speaking and writing on the phone (the live judge)
  //    and keeps accounts on the phone (plus Android backup and backup files).
  //    Fill it in to switch on the Gemini AI examiner and accounts that survive reinstalling
  //    (SETUP.md steps 3 and 4).
  aiUrl: "https://testriq.adibbhuiyan019.workers.dev",

  // 2) Optional: Firebase accounts (needed only for Google/Facebook sign-in).
  //    Your Firebase project's Web API key and project ID
  //    (Firebase console → Project settings → General → Your apps → Web app).
  //    Leave empty to use the Testriq server's accounts (aiUrl above) instead.
  firebase: {
    apiKey: "",
    authDomain: "",   // e.g. "testriq-app.firebaseapp.com"
    projectId: "",    // e.g. "testriq-app"
    appId: ""
  },

  // 3) Google sign-in WITHOUT Firebase (accounts on your Testriq server, aiUrl above).
  //    Paste your Google OAuth "Web application" client ID here, and the same ID into the
  //    Worker's GOOGLE_CLIENT_ID variable (SETUP.md step 6). Empty = no Google button.
  //    Example: "1234567890-abc123.apps.googleusercontent.com"
  googleClientId: "",

  // 4) Sign-in buttons. Google appears once googleClientId (or Firebase) is set;
  //    Facebook needs Firebase.
  googleSignIn: true,
  facebookSignIn: false,

  // 5) Public pages (GitHub Pages).
  authPage: "https://adibbhuiyann.github.io/testriq/auth.html",
  privacyUrl: "https://adibbhuiyann.github.io/testriq/privacy.html",
  deleteAccountUrl: "https://adibbhuiyann.github.io/testriq/privacy.html#delete-account",
  supportEmail: "symetree4@gmail.com",
  playStoreUrl: "https://play.google.com/store/apps/details?id=com.symetree.testriq"
};

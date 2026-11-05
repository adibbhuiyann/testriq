/* =====================================================================
   Testriq settings — edit this one file to connect your services.
   It is used by the website AND the Android app (rebuild the app after
   changing it). Everything here is safe to be public: no secret keys.
   See SETUP.md for step-by-step instructions.
   ===================================================================== */
window.TESTRIQ_CONFIG = {
  // 1) AI examiner — the URL of your Cloudflare Worker (worker.js).
  //    Example: "https://testriq-ai.adibbhuiyann.workers.dev"
  aiUrl: "",

  // 2) Accounts — your Firebase project's Web API key and project ID
  //    (Firebase console → Project settings → General → Your apps → Web app).
  //    Leave empty to use on-device accounts only (no Google/Facebook sign-in).
  firebase: {
    apiKey: "",
    authDomain: "",   // e.g. "testriq-app.firebaseapp.com"
    projectId: "",    // e.g. "testriq-app"
    appId: ""
  },

  // 3) Sign-in buttons (they only work once Firebase is set up above).
  googleSignIn: true,
  facebookSignIn: true,

  // 4) Public pages (GitHub Pages).
  authPage: "https://adibbhuiyann.github.io/testriq/auth.html",
  privacyUrl: "https://adibbhuiyann.github.io/testriq/privacy.html",
  deleteAccountUrl: "https://adibbhuiyann.github.io/testriq/privacy.html#delete-account",
  supportEmail: "symetree4@gmail.com",
  playStoreUrl: "https://play.google.com/store/apps/details?id=com.symetree.testriq"
};

/* =====================================================================
   Testriq AI examiner — Cloudflare Worker (free tier friendly)
   Examiners: Sajid Shahriar (male) and Jishad Mozmder (female).

   What it does
   - followup : the next examiner question (IELTS Part 1 / Part 3 probing,
                challenging follow-ups based on what the candidate said)
   - judge    : IELTS Speaking band scores from the full interview transcript
   - assess   : IELTS Speaking band scores from audio recordings
   - writing  : IELTS Writing Task 1 / Task 2 band scores
   - weekly   : a weekly IELTS band estimate from a week of practice
   - account.*: cloud accounts, so progress survives reinstalling the app
                (needs a D1 database bound as DB — see SETUP.md)
   - GET /    : health check (shows which model answered)

   Setup (Cloudflare dashboard → Workers → your worker → Settings → Variables and Secrets)
   - GEMINI_KEY  (Secret)   your Google AI Studio API key   ← required for the AI examiner
   - DB          (Binding)  a D1 database                    ← required for cloud accounts
   - ALLOWED     (Text)     extra allowed web origins, comma separated (optional)
   - MODEL       (Text)     preferred model for scoring (optional)
   - FAST_MODEL  (Text)     preferred model for follow-up questions (optional)
   The Android app's origin (https://appassets.androidplatform.net) and
   https://adibbhuiyann.github.io are always allowed.
   ===================================================================== */

const DEFAULT_ORIGINS = ['https://appassets.androidplatform.net', 'https://adibbhuiyann.github.io'];
const SCORING_MODELS = ['gemini-flash-latest', 'gemini-3-flash-preview', 'gemini-2.5-flash', 'gemini-flash-lite-latest', 'gemini-3.1-flash-lite'];
const FAST_MODELS = ['gemini-flash-lite-latest', 'gemini-3.1-flash-lite', 'gemini-flash-latest', 'gemini-2.5-flash-lite', 'gemini-2.5-flash'];
const EXAMINERS = { sajid: 'Sajid Shahriar', adib: 'Sajid Shahriar', jishad: 'Jishad Mozmder' };

/* ---------------------------------------------------------------- rubric */
const SPEAKING_DESCRIPTORS = `IELTS SPEAKING — the four criteria, scored as whole bands 1–9 (summarised in plain words):

FLUENCY & COHERENCE (key "fluency")
9: speaks fluently, any hesitation is about ideas not language; fully coherent, topics developed fully and appropriately.
8: fluent with only occasional repetition or self-correction; hesitation usually content-related; develops topics coherently.
7: speaks at length without noticeable effort or loss of coherence; some language-related hesitation, repetition or self-correction; uses a range of connectives and discourse markers flexibly.
6: willing to speak at length but sometimes loses coherence through repetition, self-correction or hesitation; uses a range of connectives but not always appropriately.
5: usually keeps going but relies on repetition, self-correction and/or slow speech; overuses certain connectives; simple speech fluent, complex communication causes problems.
4: cannot respond without noticeable pauses, may speak slowly with frequent repetition; links basic sentences with simple connectives; breakdowns in coherence.
3: long pauses, limited ability to link sentences, gives only simple responses.

LEXICAL RESOURCE (key "vocabulary")
9: full flexibility and precision, idiomatic language used naturally and accurately.
8: wide resource used readily and flexibly for precise meaning; less common and idiomatic vocabulary used skilfully with occasional inaccuracies; effective paraphrase.
7: flexible on a variety of topics; some less common and idiomatic vocabulary, awareness of style and collocation with some inappropriate choices; effective paraphrase.
6: wide enough vocabulary to discuss topics at length and make meaning clear despite inappropriacies; generally paraphrases successfully.
5: manages familiar and unfamiliar topics with limited flexibility; attempts paraphrase with mixed success.
4: basic vocabulary, adequate for familiar topics only; frequent inappropriate word choice; rarely attempts paraphrase.

GRAMMATICAL RANGE & ACCURACY (key "grammar")
9: full range of structures, naturally and appropriately; consistently accurate apart from native-speaker-like slips.
8: wide range used flexibly; majority of sentences error-free, only occasional inappropriacies or basic/non-systematic errors.
7: range of complex structures with some flexibility; frequently error-free sentences though some errors persist.
6: mix of simple and complex structures with limited flexibility; frequent mistakes with complex structures but these rarely impede communication.
5: basic sentence forms with reasonable accuracy; limited range of complex structures which usually contain errors.
4: basic sentence forms and some correct simple sentences; subordinate structures rare; errors frequent.

PRONUNCIATION (key "pronunciation")
9: full range of features with precision and subtlety; effortless to understand.
8: wide range of features; sustains flexible use with only occasional lapses; easy to understand; L1 accent has minimal effect.
7: shows all the positive features of 6 and some, but not all, of 8.
6: range of features with mixed control; some effective intonation and stress but not sustained; generally understood though mispronunciation of words or sounds reduces clarity at times.
5: shows all the positive features of 4 and some, but not all, of 6.
4: limited range of features; attempts at control are frequent lapses; frequent mispronunciations cause some difficulty for the listener.`;

const CALIBRATION = `CALIBRATION RULES (a fair, accurate, experienced examiner):
- USE THE FULL RANGE 3–9. Judge only the evidence in front of you. Do NOT default to 5.5–6.5: a weak candidate gets 4 or below, a strong one 7–8, a near-native one 8.5–9. Two different candidates must not get the same band unless their evidence really is equivalent.
- Anchor each criterion with these signals (they are typical, not rules):
  · Band 4: answers of a few words, long pauses, basic vocabulary only, frequent basic errors, almost no linking.
  · Band 5: answers usually extended but repetitive; slow or hesitant; simple linking (and, but, because); complex sentences rare and often wrong.
  · Band 6: willing to speak at length with some loss of coherence; enough vocabulary to discuss topics; mix of simple and complex sentences with noticeable errors.
  · Band 7: speaks at length easily; some less common words and idioms; frequent error-free complex sentences; flexible linking.
  · Band 8: fluent with rare hesitation; wide, precise, idiomatic vocabulary; wide range of structures, mostly error-free.
  · Band 9: native-like flexibility, precision and accuracy throughout.
- Score each criterion independently. It is normal for criteria to differ by 1–2 bands.
- Very short answers (Part 1 under ~15 words, Part 3 under ~30 words, Part 2 under ~1 minute or ~100 words) limit Fluency & Coherence to 5 or below.
- Memorised or off-topic speech limits Fluency & Coherence and Lexical Resource.
- Less than ~60 words of total speech = insufficient evidence: set sufficientEvidence false and confidence "low", and keep scores cautious (4–5).
- The transcript comes from automatic speech recognition: punctuation, capitals and fillers may be missing or wrong, and recognition mistakes can look like grammar or word errors. Do not penalise punctuation. Only treat something as a grammar/vocabulary error when it is clearly the candidate's own error.
- Pronunciation from text: use the recognition confidence, the speaking rate and pauses. Very high confidence (≥0.92) with a natural rate (120–170 wpm) and few pauses suggests 7–8; ≥0.85 suggests 6–7; low confidence (<0.7) or many garbled words suggests 5 or below. If the answers were typed, pronunciation must be null.
- Fluency from timing: the app measured how long the candidate took to start answering (response delay) and how many long pauses (over ~2 seconds) they made. Long delays (over 4 s) and frequent pauses (over 3 a minute) mean hesitation; under 2 s and under 1 pause a minute mean fluent delivery.
- Quote the candidate's actual words as evidence. Corrections must quote the exact words used.`;

/* ---------------------------------------------------------------- helpers */
const json = (data, status, cors) =>
  new Response(JSON.stringify(data), { status, headers: { ...cors, 'Content-Type': 'application/json' } });
const clamp = (x, a, b) => Math.max(a, Math.min(b, x));
const str = (v, max = 600) => (typeof v === 'string' ? v.trim().slice(0, max) : '');
const arr = (v, max) => (Array.isArray(v) ? v.slice(0, max) : []);
const band = (v) => {
  const n = Number(v);
  return Number.isFinite(n) ? clamp(Math.round(n * 2) / 2, 0, 9) : null;
};
const wholeBand = (v) => {
  const n = Number(v);
  return Number.isFinite(n) ? clamp(Math.round(n), 1, 9) : null;
};
/** Official IELTS rounding of an average to the nearest half band (.25 → .5, .75 → next whole). */
const ieltsRound = (x) => {
  const f = Math.floor(x),
    d = x - f;
  return d < 0.25 ? f : d < 0.75 ? f + 0.5 : f + 1;
};
const sanitizeTurns = (messages) =>
  arr(messages, 40)
    .map((m) => ({ role: m?.role === 'user' ? 'Candidate' : 'Examiner', text: str(m?.content, 1500) }))
    .filter((m) => m.text);

function toBase64(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
  return btoa(binary);
}

/** Calls Gemini, trying several models until one answers. Returns { data, model }. */
async function gemini(env, parts, { fast = false, temperature = 0.3, maxTokens = 4096, timeout = 55000 } = {}) {
  if (!env.GEMINI_KEY) throw Object.assign(new Error('GEMINI_KEY is not set on the Worker'), { status: 503 });
  const preferred = fast ? env.FAST_MODEL : env.MODEL;
  const models = [...new Set([preferred, ...(fast ? FAST_MODELS : SCORING_MODELS)].filter(Boolean))];
  let lastError = 'no model answered';
  const started = Date.now();
  for (const model of models) {
    const left = timeout - (Date.now() - started);
    if (left < 2500) break;
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), left);
    try {
      const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
        method: 'POST',
        signal: ctrl.signal,
        headers: { 'Content-Type': 'application/json', 'x-goog-api-key': env.GEMINI_KEY },
        body: JSON.stringify({
          contents: [{ role: 'user', parts }],
          generationConfig: { responseMimeType: 'application/json', temperature, maxOutputTokens: maxTokens },
        }),
      });
      const body = await r.json().catch(() => ({}));
      if (!r.ok) {
        lastError = `${model}: ${r.status} ${body?.error?.message || ''}`.slice(0, 300);
        continue; // 404 unknown model, 429 quota, 5xx busy → try the next model
      }
      const text = (body.candidates?.[0]?.content?.parts || []).map((p) => p.text || '').join('');
      const data = parseJSON(text);
      if (data) return { data, model };
      lastError = `${model}: unreadable answer`;
    } catch (e) {
      lastError = `${model}: ${e.name === 'AbortError' ? 'timed out' : e.message}`;
    } finally {
      clearTimeout(timer);
    }
  }
  throw Object.assign(new Error(lastError), { status: 502 });
}

function parseJSON(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {}
  const m = text.match(/\{[\s\S]*\}/);
  if (m)
    try {
      return JSON.parse(m[0]);
    } catch {}
  return null;
}

/* ---------------------------------------------------------------- follow-up */
async function followup(b, env) {
  const examiner = EXAMINERS[b.examinerId] || EXAMINERS.sajid;
  const turns = sanitizeTurns(b.messages);
  const part = str(b.part, 80) || 'Part 1';
  const lastAnswer = [...turns].reverse().find((t) => t.role === 'Candidate')?.text || '';
  const asked = turns.filter((t) => t.role === 'Examiner').map((t) => t.text);
  const isP3 = /part 3|discussion/i.test(part);
  const isP2 = /part 2|rounding/i.test(part);
  const memory = arr(b.memory, 20).map((m) => str(m, 160)).filter(Boolean);
  const role = b.role === 'student' ? 'a student' : b.role === 'worker' ? 'working (not a student)' : 'unknown';
  const prompt = `You are ${examiner}, a real, experienced IELTS Speaking examiner conducting a live test. Stay in role.
Current section: ${part}. The scripted question was: "${str(b.question, 400)}".
Candidate's name or nickname: ${str(b.nickname, 40) || 'unknown'}. Work or study: ${role}.
What you remember about the candidate from earlier in this test:
${memory.length ? memory.map((m) => `- ${m}`).join('\n') : '- nothing yet'}

Conversation so far (most recent last):
${turns.map((t) => `${t.role}: ${t.text}`).join('\n')}

Write the ONE next follow-up question to ask about the candidate's LAST answer ("${lastAnswer.slice(0, 600)}").
How a real examiner does this:
${
  isP3
    ? `- Part 3 is an abstract discussion. Push the candidate's reasoning: challenge their view, play devil's advocate, ask "why", ask them to compare, speculate or evaluate.
- About half the time, politely CONTRADICT what they said: "But some people would argue the opposite — that ... What would you say to them?" / "Isn't it also true that ...?"`
    : isP2
      ? `- This is the rounding-off question after the long turn: one brief, simple question connected with their talk.`
      : `- Part 1 is a friendly interview about familiar topics. Go gradually deeper than the scripted question.
- Pick up a specific detail they mentioned (a place, a person, a preference) and ask about it.
- Sometimes (about 1 in 3) gently CHALLENGE or contradict their answer to test flexibility: "You said you prefer X — but many people find Y better. Why do you disagree?" / "Really? I'd have thought ... Why is that?"`
}
- You REMEMBER everything said in this test. When it fits naturally (about 1 time in 3), link your question to something they said EARLIER: "Earlier you mentioned that you ... — ..." Never invent things they did not say.
- If the LAST answer contradicts something they said earlier, politely point it out and ask them to explain: "A moment ago you said ..., but now you say ... — which is it?"
- If the LAST answer did not actually answer the question (for example, they did not choose between the two options, or went off topic), politely bring them back to the question.
- If the answer was very short or vague, ask them to explain why or to give an example.
- If they said they don't know, offer an easier angle.
- Keep work/study questions consistent with what they do (do not ask a student about their job).
- Never repeat or rephrase a question already asked: ${JSON.stringify(asked.slice(-8))}
- Never give feedback, praise, corrections or band scores. No greetings.
- Sound like a real person talking, not a textbook: natural spoken British English with contractions (you've, what's, I'd, don't), everyday words and a relaxed rhythm. Vary how you start questions (And…, So…, What about…, Why do you think…, How come…). At most 30 words.
- First, a very short neutral spoken acknowledgement of what they said (1–4 words, e.g. "Right.", "I see.", "Okay.", "Mm, interesting.", "Fair enough."). Never praise or judge ("great answer" is forbidden). Vary it; don't reuse: ${JSON.stringify(arr(b.recentAcks, 6).map((x) => str(x, 30)))}
Return JSON: {"ack":"...","question":"...","kind":"challenge"|"probe"|"extend"|"recall"|"clarify"}`;
  const { data, model } = await gemini(env, [{ text: prompt }], { fast: true, temperature: 0.85, maxTokens: 300, timeout: 12000 });
  const q = str(data.question, 300).replace(/^["“]|["”]$/g, '');
  if (!q || q.split(/\s+/).length < 3) throw Object.assign(new Error('empty question'), { status: 502 });
  let ack = str(data.ack, 40).replace(/^["“]|["”]$/g, '');
  if (ack.split(/\s+/).length > 5 || /\b(great|excellent|good answer|well done|perfect|brilliant|wonderful)\b/i.test(ack)) ack = '';
  if (ack && !/[.!?]$/.test(ack)) ack += '.';
  return { question: q, ack, kind: ['challenge', 'probe', 'extend', 'recall', 'clarify'].includes(data.kind) ? data.kind : 'probe', model };
}

/* ---------------------------------------------------------------- speaking: shape the answer */
function shapeSpeaking(data, { typed, exam = 'IELTS', source }) {
  const keys = ['fluency', 'vocabulary', 'grammar', 'pronunciation'];
  const crit = keys.map((key) => {
    const c = arr(data.criteria, 8).find((x) => x && x.key === key) || {};
    let score = wholeBand(c.score);
    if (key === 'pronunciation' && typed) score = null;
    return {
      key,
      score,
      observation: str(c.observation, 900) || 'No observation was returned.',
      practice: str(c.practice, 500) || 'Keep practising this skill with new questions.',
    };
  });
  const scored = crit.map((c) => c.score).filter((s) => s != null);
  const overall = scored.length ? ieltsRound(scored.reduce((a, b) => a + b, 0) / scored.length) : null;
  const sufficient = data.sufficientEvidence !== false;
  return {
    version: 3,
    source,
    exam,
    scale: 9,
    audioAssessed: source === 'audio-ai',
    sufficientEvidence: sufficient,
    confidence: ['low', 'medium', 'high'].includes(data.confidence) ? data.confidence : sufficient ? 'medium' : 'low',
    overall,
    summary: str(data.summary, 900) || 'Your speaking has been assessed against the four IELTS criteria.',
    criteria: crit,
    strengths: arr(data.strengths, 5).map((s) => str(s, 300)).filter(Boolean),
    improvements: arr(data.improvements, 5)
      .map((i) => ({ issue: str(i?.issue, 120), evidence: str(i?.evidence, 400), action: str(i?.action, 400) }))
      .filter((i) => i.issue && i.action),
    corrections: arr(data.corrections, 8)
      .map((c) => ({ said: str(c?.said, 300), better: str(c?.better, 300), why: str(c?.why, 200) }))
      .filter((c) => c.said && c.better),
    plan: arr(data.plan, 7)
      .map((p) => ({ day: str(p?.day, 40), task: str(p?.task, 400) }))
      .filter((p) => p.day && p.task),
    limitations: arr(data.limitations, 5).map((s) => str(s, 300)).filter(Boolean),
    nextBandTips: str(data.nextBandTips, 600),
    created: new Date().toISOString(),
  };
}

const SPEAKING_OUTPUT = `Return ONLY this JSON:
{"criteria":[{"key":"fluency","observation":"2-3 sentences quoting evidence (write this FIRST, then decide the score)","score":3-9 whole number,"practice":"one specific exercise"},{"key":"vocabulary",...},{"key":"grammar",...},{"key":"pronunciation","observation":"...","score":whole number or null,...}],
"summary":"3 sentences: overall level, the main reason, the most important next step",
"sufficientEvidence":true|false,"confidence":"low"|"medium"|"high",
"strengths":["specific, quoting the candidate"] (2-4),
"improvements":[{"issue":"short title","evidence":"quote or fact from the answers","action":"concrete practice"}] (2-4),
"corrections":[{"said":"exact words used","better":"natural correct version","why":"short reason"}] (up to 6, only real errors),
"plan":[{"day":"Day 1","task":"..."}] (5-7 days, specific to this candidate),
"nextBandTips":"what exactly would move this candidate up half a band",
"limitations":["honest limits of this assessment"] (1-3)}`;

/* ---------------------------------------------------------------- judge (transcript) */
async function judge(b, env) {
  const answers = arr(b.answers, 40).map((a) => ({
    part: str(a?.part, 60),
    question: str(a?.question, 500),
    answer: str(a?.answer, 3000),
    seconds: Number(a?.seconds) || 0,
    words: Number(a?.words) || 0,
    confidence: a?.confidence == null ? null : Number(a.confidence),
    delay: a?.delay == null || !Number.isFinite(Number(a.delay)) ? null : Math.round(Number(a.delay) * 10) / 10,
    pauses: Number(a?.pauses) || 0,
  }));
  const spoken = answers.filter((a) => a.answer && a.answer !== '(No answer)');
  const typed = !!b.typed;
  const m = b.metrics || {};
  const prompt = `You are ${EXAMINERS[b.examinerId] || EXAMINERS.sajid}, a certified IELTS Speaking examiner. Assess this candidate's complete speaking test.

${SPEAKING_DESCRIPTORS}

${CALIBRATION}

MEASURED FACTS (computed by the app, trust these):
- Answers given: ${spoken.length}; total words: ${m.words ?? 'unknown'}; average words per answer: ${m.avgWords ?? 'unknown'}
- Speaking rate: ${m.wpm ? m.wpm + ' words per minute' : 'not measured'}; filler words (um/uh/you know): ${m.fillers ?? 0}
- Long pauses per minute: ${m.pausesPerMinute ?? 'not measured'}; average delay before answering: ${m.responseDelay != null ? m.responseDelay + ' s' : 'not measured'}
- Speech-recognition confidence (0–1): ${m.confidence != null ? Number(m.confidence).toFixed(2) : 'not available'}
- Linking words detected: ${arr(m.linking, 15).map((x) => str(x, 40)).join(', ') || 'none'}
- Less common words detected: ${arr(m.lessCommonWords, 15).map((x) => str(x, 40)).join(', ') || 'none'}; idiomatic expressions: ${arr(m.idioms, 10).map((x) => str(x, 60)).join(', ') || 'none'}
- Grammar structures detected: ${arr(m.structures, 20).map((x) => str(x, 40)).join(', ') || 'mostly simple'}
- Possible slips flagged automatically (check them yourself): ${arr(m.likelySlips, 10).map((x) => str(x, 80)).join('; ') || 'none'}
- Part 2 long turn: ${m.part2 ? `${Number(m.part2.words) || 0} words in ${Number(m.part2.sec) || 0} s` : 'not in this test'}
- Answers were ${typed ? 'TYPED (so pronunciation must be null and fluency judged from organisation only)' : 'SPOKEN and transcribed automatically'}

THE TEST (question → candidate's answer):
${answers.map((a, i) => `#${i + 1} [${a.part}] Q: ${a.question}\nA: ${a.answer || '(no answer)'}${a.seconds ? ` (${Math.round(a.seconds)} s, ${a.words} words${a.confidence != null && a.confidence >= 0 ? `, recognition ${a.confidence.toFixed(2)}` : ''}${a.delay != null ? `, started after ${a.delay} s` : ''}${a.pauses ? `, ${a.pauses} long pause${a.pauses === 1 ? '' : 's'}` : ''})` : ''}`).join('\n\n')}

${SPEAKING_OUTPUT}`;
  const { data, model } = await gemini(env, [{ text: prompt }], { temperature: 0.35, maxTokens: 6000 });
  const assessment = shapeSpeaking(data, { typed, source: 'ai-transcript' });
  assessment.model = model;
  if (!typed && (m.confidence == null || m.confidence < 0))
    assessment.limitations.push('Pronunciation was estimated from the transcript only; record your answers for a fuller check.');
  return { assessment };
}

/* ---------------------------------------------------------------- assess (audio) */
async function assessAudio(form, env) {
  const files = form.getAll('audio').filter((f) => f && typeof f.arrayBuffer === 'function');
  if (!files.length) throw Object.assign(new Error('No audio received'), { status: 400 });
  let total = 0;
  const parts = [];
  let meta = [];
  try {
    meta = JSON.parse(form.get('metadata') || '[]');
  } catch {}
  for (const [i, f] of files.entries()) {
    const buf = await f.arrayBuffer();
    total += buf.byteLength;
    if (total > 14_000_000) throw Object.assign(new Error('Audio too large'), { status: 413 });
    parts.push({ text: `Recording ${i + 1} — question: ${str(meta[i]?.question, 400) || 'speaking task'}` });
    parts.push({ inline_data: { mime_type: (f.type || 'audio/wav').split(';')[0], data: toBase64(buf) } });
  }
  const prompt = `You are ${EXAMINERS[form.get('examinerId')] || EXAMINERS.sajid}, a certified IELTS Speaking examiner. Listen to the candidate's recordings and assess them.
Transcribe what you hear, then judge Fluency & Coherence, Lexical Resource, Grammatical Range & Accuracy and Pronunciation FROM THE AUDIO (pronunciation must be judged from what you hear: sounds, word stress, rhythm, intonation, chunking).

${SPEAKING_DESCRIPTORS}

${CALIBRATION.replace(/- The transcript comes from automatic speech recognition[^\n]*\n/, '').replace(/- Pronunciation from text[^\n]*\n/, '')}

Candidate's own transcript (may be incomplete): ${str(form.get('transcript'), 6000) || '(none)'}

${SPEAKING_OUTPUT.replace('"limitations"', '"transcript":"what you heard, all recordings","limitations"')}`;
  const { data, model } = await gemini(env, [{ text: prompt }, ...parts], { temperature: 0.2, maxTokens: 7000, timeout: 85000 });
  const a = shapeSpeaking(data, { typed: false, source: 'audio-ai', exam: str(form.get('exam'), 10) || 'IELTS' });
  a.transcript = str(data.transcript, 16000);
  a.model = model;
  if (!a.limitations.length) a.limitations = ['A practice estimate from an AI examiner, not an official IELTS result.'];
  return { assessment: a };
}

/* ---------------------------------------------------------------- writing */
async function writing(b, env) {
  const task = b.task === 'ielts-t1' ? 'ielts-t1' : 'ielts-t2';
  const t1 = task === 'ielts-t1';
  const response = str(b.response, 12000);
  const words = (response.match(/\S+/g) || []).length;
  const prompt = `You are a certified IELTS Writing examiner. Mark this IELTS Academic Writing ${t1 ? 'Task 1 (report on a visual, minimum 150 words)' : 'Task 2 (essay, minimum 250 words)'} strictly against the public band descriptors.

Criteria (whole bands 1–9):
- ${t1 ? '"ta" Task Achievement: covers requirements, clear overview of main trends/differences/stages, key features highlighted and accurately supported with data, no opinion.' : '"tr" Task Response: addresses all parts of the task, clear position throughout, ideas extended and supported, relevant.'}
- "cc" Coherence & Cohesion: logical organisation, paragraphing, a range of cohesive devices used appropriately (not mechanical), referencing.
- "lr" Lexical Resource: range, precision, collocation, less common vocabulary, spelling and word formation.
- "gr" Grammatical Range & Accuracy: mix of simple and complex structures, proportion of error-free sentences, punctuation.

Use the full range 3–9 and judge only the evidence: weak scripts get 4 or below, strong ones 7–8, near-native ones 8.5–9. Criteria often differ by 1–2 bands. Under the word minimum: ${t1 ? 'Task Achievement' : 'Task Response'} is penalised (under ${t1 ? 150 : 250} words → max 5 for that criterion; under ${t1 ? 100 : 170} words → max 4). Memorised, off-topic or copied-prompt text is penalised. ${t1 ? 'No overview → Task Achievement max 5.' : 'No clear position when asked → Task Response max 5.'} Each criterion is judged independently.

THE TASK:
${str(b.prompt, 3000)}
${b.visual ? 'Key data shown in the visual: ' + str(b.visual, 2500) : ''}

THE CANDIDATE'S RESPONSE (${words} words):
"""${response}"""

Return ONLY this JSON:
{"criteria":[{"key":"${t1 ? 'ta' : 'tr'}","observation":"2-3 sentences with quotes (write this first)","score":n,"practice":"one specific next step"},{"key":"cc",...},{"key":"lr",...},{"key":"gr",...}],
"summary":"3 sentences","strengths":["..."] (2-4),"improvements":[{"issue":"...","action":"..."}] (2-4),
"corrections":[{"excerpt":"exact text from the response","fix":"corrected text","why":"short reason","kind":"grammar"|"spelling"|"punctuation"|"vocabulary"}] (up to 12, real errors only),
"rewrite":"an improved version of ONE weak sentence or paragraph from the response, at band 8 level",
"confidence":"low"|"medium"|"high"}`;
  const { data, model } = await gemini(env, [{ text: prompt }], { temperature: 0.2, maxTokens: 7000 });
  const labels = { ta: 'Task achievement', tr: 'Task response', cc: 'Coherence & cohesion', lr: 'Lexical resource', gr: 'Grammatical range & accuracy' };
  const keys = [t1 ? 'ta' : 'tr', 'cc', 'lr', 'gr'];
  const criteria = keys.map((key) => {
    const c = arr(data.criteria, 8).find((x) => x && x.key === key) || {};
    return { key, label: labels[key], score: wholeBand(c.score) ?? 5, observation: str(c.observation, 900), practice: str(c.practice, 500) };
  });
  return {
    assessment: {
      source: 'ai',
      model,
      overall: ieltsRound(criteria.reduce((n, c) => n + c.score, 0) / 4),
      criteria,
      summary: str(data.summary, 900),
      strengths: arr(data.strengths, 5).map((s) => str(s, 300)).filter(Boolean),
      improvements: arr(data.improvements, 5).map((i) => ({ issue: str(i?.issue, 120), action: str(i?.action, 400) })).filter((i) => i.issue),
      corrections: arr(data.corrections, 15)
        .map((c) => ({ excerpt: str(c?.excerpt, 200), fix: str(c?.fix, 200), why: str(c?.why, 200), kind: ['grammar', 'spelling', 'punctuation', 'vocabulary'].includes(c?.kind) ? c.kind : 'grammar' }))
        .filter((c) => c.excerpt && c.fix),
      rewrite: str(data.rewrite, 1500),
      confidence: ['low', 'medium', 'high'].includes(data.confidence) ? data.confidence : 'medium',
    },
  };
}

/* ---------------------------------------------------------------- weekly estimate */
async function weekly(b, env) {
  const s = b.summary || {};
  const prompt = `You are a senior IELTS examiner and coach. A learner has finished one week of daily practice in the Testriq app. Estimate their CURRENT IELTS Academic band for each skill and overall, from the evidence below.

Rules:
- Be realistic and strict; do not inflate. Base each skill only on its evidence. Small samples (1–2 short tests) must be treated cautiously: move towards the middle of the evidence, mark confidence "low".
- Listening/Reading: the app gives raw accuracy and the official raw-score conversion; short practice sets are easier than a full paper, so a skill band can be up to 0.5 below the converted value when samples are small.
- Writing/Speaking: use the examiner band scores given (AI or on-device estimates).
- A skill with no evidence gets band null.
- Consider consistency and trend (improving or declining) across the week.

Target band: ${str(b.target, 20) || 'not set'}. Days the daily goal was completed: ${Number(b.daysCompleted) || 0} of 7.

EVIDENCE:
${JSON.stringify(s).slice(0, 12000)}

Return ONLY this JSON:
{"skills":{"listening":band or null,"reading":band or null,"writing":band or null,"speaking":band or null} (half bands allowed),
"confidence":"low"|"medium"|"high",
"summary":"3-4 sentences: where they stand, the gap to the target, the single most important priority",
"skillNotes":{"listening":"1-2 sentences","reading":"...","writing":"...","speaking":"..."},
"strengths":["..."] (2-3),"priorities":["..."] (2-4, concrete),
"nextWeek":[{"day":"Monday","task":"..."}] (7 entries, realistic for the target)}`;
  const { data, model } = await gemini(env, [{ text: prompt }], { temperature: 0.2, maxTokens: 5000 });
  const skills = {};
  for (const k of ['listening', 'reading', 'writing', 'speaking']) skills[k] = data.skills?.[k] == null ? null : band(data.skills[k]);
  const have = Object.values(skills).filter((v) => v != null);
  return {
    weekly: {
      source: 'ai',
      model,
      skills,
      overall: have.length ? ieltsRound(have.reduce((a, c) => a + c, 0) / have.length) : null,
      complete: have.length === 4,
      confidence: ['low', 'medium', 'high'].includes(data.confidence) ? data.confidence : 'low',
      summary: str(data.summary, 1200),
      skillNotes: Object.fromEntries(['listening', 'reading', 'writing', 'speaking'].map((k) => [k, str(data.skillNotes?.[k], 400)])),
      strengths: arr(data.strengths, 4).map((x) => str(x, 300)).filter(Boolean),
      priorities: arr(data.priorities, 5).map((x) => str(x, 300)).filter(Boolean),
      nextWeek: arr(data.nextWeek, 7).map((d) => ({ day: str(d?.day, 30), task: str(d?.task, 300) })).filter((d) => d.day && d.task),
      created: new Date().toISOString(),
    },
  };
}

/* ---------------------------------------------------------------- cloud accounts (Cloudflare D1)
   Accounts and progress are stored in a D1 database bound to this Worker as DB, so a learner
   who deletes the app, changes phone or reinstalls can log in and get everything back.
   Passwords are hashed with PBKDF2-SHA256 (100,000 rounds, random salt); session tokens are
   random and only their SHA-256 hash is stored. */
const SESSION_DAYS = 400;
const MAX_DATA = 1_800_000;
let schemaReady = false;
const hex = (buf) => Array.from(new Uint8Array(buf), (b) => b.toString(16).padStart(2, '0')).join('');
const randomHex = (n) => hex(crypto.getRandomValues(new Uint8Array(n)));
const sha256 = async (s) => hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s)));
const fail = (status, code, message) => Object.assign(new Error(message || code), { status, code });
const sameText = (a, b) => {
  if (typeof a !== 'string' || typeof b !== 'string' || a.length !== b.length) return false;
  let d = 0;
  for (let i = 0; i < a.length; i++) d |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return d === 0;
};
async function pbkdf2(password, salt) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits({ name: 'PBKDF2', hash: 'SHA-256', salt: new TextEncoder().encode(salt), iterations: 100000 }, key, 256);
  return hex(bits);
}
async function database(env) {
  if (!env.DB) throw fail(501, 'NO_DATABASE', 'Cloud accounts are not switched on for this server yet.');
  if (!schemaReady) {
    await env.DB.batch([
      env.DB.prepare('CREATE TABLE IF NOT EXISTS users (email TEXT PRIMARY KEY, uid TEXT NOT NULL UNIQUE, name TEXT, salt TEXT NOT NULL, hash TEXT NOT NULL, created INTEGER, updated INTEGER, data TEXT, fails INTEGER DEFAULT 0, locked INTEGER DEFAULT 0)'),
      env.DB.prepare('CREATE TABLE IF NOT EXISTS sessions (th TEXT PRIMARY KEY, uid TEXT NOT NULL, created INTEGER, expires INTEGER)'),
      env.DB.prepare('CREATE INDEX IF NOT EXISTS sessions_uid ON sessions(uid)'),
    ]);
    schemaReady = true;
  }
  return env.DB;
}
const cleanEmail = (e) => {
  const v = str(e, 254).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v)) throw fail(400, 'INVALID_EMAIL', 'Enter a valid email address.');
  return v;
};
const checkPassword = (p) => {
  if (typeof p !== 'string' || p.length < 6) throw fail(400, 'WEAK_PASSWORD', 'Choose a password of at least 6 characters.');
  if (p.length > 200) throw fail(400, 'WEAK_PASSWORD', 'That password is too long.');
  return p;
};
async function newSession(DB, uid) {
  const token = randomHex(32);
  const now = Date.now();
  await DB.prepare('INSERT INTO sessions (th, uid, created, expires) VALUES (?, ?, ?, ?)').bind(await sha256(token), uid, now, now + SESSION_DAYS * 864e5).run();
  return token;
}
async function userFor(DB, token) {
  if (typeof token !== 'string' || token.length !== 64) throw fail(401, 'SESSION_EXPIRED', 'Please log in again.');
  const s = await DB.prepare('SELECT uid, expires FROM sessions WHERE th = ?').bind(await sha256(token)).first();
  if (!s || s.expires < Date.now()) throw fail(401, 'SESSION_EXPIRED', 'Please log in again.');
  const u = await DB.prepare('SELECT * FROM users WHERE uid = ?').bind(s.uid).first();
  if (!u) throw fail(401, 'SESSION_EXPIRED', 'Please log in again.');
  return u;
}
const publicUser = (u, token) => ({ token, uid: u.uid, email: u.email, name: u.name || '', updated: u.updated || 0 });
const checkData = (d) => {
  const s = typeof d === 'string' ? d : JSON.stringify(d ?? null);
  if (s.length > MAX_DATA) throw fail(413, 'TOO_LARGE', 'Your saved progress is too large to sync.');
  return s;
};

async function account(action, b, env) {
  const DB = await database(env);
  const now = Date.now();
  if (action === 'account.signup') {
    const email = cleanEmail(b.email);
    const password = checkPassword(b.password);
    const name = str(b.name, 60);
    if (await DB.prepare('SELECT 1 FROM users WHERE email = ?').bind(email).first()) throw fail(409, 'EMAIL_EXISTS', 'An account with this email already exists. Log in instead.');
    const uid = 'c-' + randomHex(12);
    const salt = randomHex(16);
    const data = b.data == null ? null : checkData(b.data);
    await DB.prepare('INSERT INTO users (email, uid, name, salt, hash, created, updated, data) VALUES (?, ?, ?, ?, ?, ?, ?, ?)')
      .bind(email, uid, name, salt, await pbkdf2(password, salt), now, data ? now : 0, data).run();
    return { ...publicUser({ uid, email, name, updated: data ? now : 0 }, await newSession(DB, uid)), data: null, created: true };
  }
  if (action === 'account.login') {
    const email = cleanEmail(b.email);
    const password = checkPassword(b.password);
    const u = await DB.prepare('SELECT * FROM users WHERE email = ?').bind(email).first();
    if (!u) throw fail(404, 'NOT_FOUND', 'No account uses this email yet.');
    if (u.locked && u.locked > now) throw fail(429, 'TOO_MANY_ATTEMPTS', 'Too many attempts. Please wait a few minutes and try again.');
    if (!sameText(await pbkdf2(password, u.salt), u.hash)) {
      const fails = (u.fails || 0) + 1;
      await DB.prepare('UPDATE users SET fails = ?, locked = ? WHERE email = ?').bind(fails >= 8 ? 0 : fails, fails >= 8 ? now + 15 * 60e3 : 0, email).run();
      throw fail(401, 'WRONG_PASSWORD', 'The email or password is incorrect.');
    }
    if (u.fails) await DB.prepare('UPDATE users SET fails = 0, locked = 0 WHERE email = ?').bind(email).run();
    return { ...publicUser(u, await newSession(DB, u.uid)), data: u.data || null };
  }
  const u = await userFor(DB, b.token);
  if (action === 'account.pull') return { ...publicUser(u, undefined), data: u.data || null };
  if (action === 'account.push') {
    const data = checkData(b.data);
    const name = b.name == null ? u.name : str(b.name, 60);
    await DB.prepare('UPDATE users SET data = ?, updated = ?, name = ? WHERE uid = ?').bind(data, now, name, u.uid).run();
    return { ok: true, updated: now };
  }
  if (action === 'account.logout') {
    await DB.prepare('DELETE FROM sessions WHERE th = ?').bind(await sha256(b.token)).run();
    return { ok: true };
  }
  if (action === 'account.password') {
    if (!sameText(await pbkdf2(checkPassword(b.password), u.salt), u.hash)) throw fail(401, 'WRONG_PASSWORD', 'Your current password is incorrect.');
    const salt = randomHex(16);
    await DB.prepare('UPDATE users SET salt = ?, hash = ? WHERE uid = ?').bind(salt, await pbkdf2(checkPassword(b.newPassword), salt), u.uid).run();
    return { ok: true };
  }
  if (action === 'account.delete') {
    if (!sameText(await pbkdf2(checkPassword(b.password), u.salt), u.hash)) throw fail(401, 'WRONG_PASSWORD', 'Your password is incorrect.');
    await DB.batch([DB.prepare('DELETE FROM sessions WHERE uid = ?').bind(u.uid), DB.prepare('DELETE FROM users WHERE uid = ?').bind(u.uid)]);
    return { ok: true, deleted: true };
  }
  throw fail(400, 'UNKNOWN_ACTION', 'Unknown action');
}

/* ---------------------------------------------------------------- router */
export default {
  async fetch(req, env) {
    const origin = req.headers.get('Origin') || '';
    const allowed = [...DEFAULT_ORIGINS, ...(env.ALLOWED || '').split(',').map((x) => x.trim()).filter(Boolean)];
    const ok = !origin || allowed.includes(origin) || /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(origin);
    const cors = {
      'Access-Control-Allow-Origin': ok && origin ? origin : allowed[0],
      'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
      'Access-Control-Max-Age': '86400',
      Vary: 'Origin',
    };
    if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors });
    if (!ok) return json({ error: 'Origin not allowed' }, 403, cors);
    if (req.method === 'GET') {
      return json({ ok: true, service: 'Testriq AI examiner', version: '1.0.2', configured: !!env.GEMINI_KEY, accounts: !!env.DB, models: SCORING_MODELS }, 200, cors);
    }
    if (req.method !== 'POST') return json({ error: 'Method not allowed' }, 405, cors);
    try {
      const type = req.headers.get('Content-Type') || '';
      if (type.includes('multipart/form-data')) {
        const form = await req.formData();
        if (form.get('action') !== 'assess') return json({ error: 'Unknown action' }, 400, cors);
        return json(await assessAudio(form, env), 200, cors);
      }
      const text = await req.text();
      if (text.length > 400_000) return json({ error: 'Request too large' }, 413, cors);
      let b;
      try {
        b = JSON.parse(text);
      } catch {
        return json({ error: 'Bad JSON' }, 400, cors);
      }
      if (typeof b.action === 'string' && b.action.startsWith('account.')) return json(await account(b.action, b, env), 200, cors);
      switch (b.action) {
        case 'ping': {
          const r = await gemini(env, [{ text: 'Return {"ok":true}' }], { fast: true, maxTokens: 20, timeout: 15000 });
          return json({ ok: true, model: r.model }, 200, cors);
        }
        case 'followup':
          return json(await followup(b, env), 200, cors);
        case 'judge':
          return json(await judge(b, env), 200, cors);
        case 'writing':
          return json(await writing(b, env), 200, cors);
        case 'weekly':
          return json(await weekly(b, env), 200, cors);
        default:
          return json({ error: 'Unknown action' }, 400, cors);
      }
    } catch (e) {
      const status = e.status || 500;
      return json({ error: String(e.message || e).slice(0, 300), code: e.code || undefined }, status, cors);
    }
  },
};

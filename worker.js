// Cloudflare Worker: Adib Bhuiyan's AI judge. Keeps your Gemini key secret. Free tier friendly.
// Settings > Variables and Secrets:  GEMINI_KEY (secret) · ALLOWED = https://adibbhuiyann.github.io · MODEL (optional, a free-tier Gemini model with audio input)
const RUBRIC = `You are Adib Bhuiyan, a strict, fair, experienced IELTS Speaking examiner (also a PTE Speaking coach). Judge ONLY what the candidate actually said. Do not inflate scores; most learners are band 5-7. Use the official IELTS descriptors: Fluency & Coherence, Lexical Resource, Grammatical Range & Accuracy, Pronunciation (judge from the AUDIO only; if audio is missing or unclear, set pronunciation to 0). For PTE tasks use the same 0-9 scale as an estimate. Return ONLY JSON: {"transcript":"exact words heard","fluency":0-9 in 0.5 steps,"lexical":..,"grammar":..,"pronunciation":..,"errors":[{"said":"exact quote","better":"corrected version","why":"short reason"}] (max 4),"strengths":["short"] (max 2),"tips":["specific, actionable"] (max 3),"followUp":"one natural examiner follow-up question about what they said, or empty string for Part 2 / PTE / intro"}`;
export default {
  async fetch(req, env) {
    const origin = req.headers.get('Origin') || '';
    const ok = (env.ALLOWED || '').split(',').map(x => x.trim()).includes(origin) || /^https?:\/\/localhost(:\d+)?$/.test(origin);
    const cors = { 'Access-Control-Allow-Origin': ok ? origin : 'null', 'Access-Control-Allow-Methods': 'POST,OPTIONS', 'Access-Control-Allow-Headers': 'Content-Type', Vary: 'Origin' };
    if (req.method === 'OPTIONS') return new Response(null, { headers: cors });
    if (req.method !== 'POST' || !ok) return new Response('Forbidden', { status: 403, headers: cors });
    let b; try { b = await req.json(); } catch { return new Response('Bad request', { status: 400, headers: cors }); }
    if ((b.audio || '').length > 6_000_000) return new Response('Too large', { status: 413, headers: cors });
    const parts = [{ text: `${RUBRIC}\nExam: ${b.exam}. Part: ${b.part}. Question asked: ${b.question}` }];
    if (b.audio) parts.push({ inline_data: { mime_type: String(b.mime || 'audio/webm').split(';')[0], data: b.audio } });
    else parts.push({ text: 'Typed answer (no audio): ' + String(b.text || '').slice(0, 4000) });
    const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${env.MODEL || 'gemini-3.1-flash-lite'}:generateContent`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', 'x-goog-api-key': env.GEMINI_KEY },
      body: JSON.stringify({ contents: [{ parts }], generationConfig: { responseMimeType: 'application/json', temperature: 0.3 } }) });
    const j = await r.json().catch(() => ({}));
    const txt = (j.candidates?.[0]?.content?.parts || []).map(p => p.text || '').join('');
    return new Response(txt || JSON.stringify({ error: j.error || 'empty' }), { status: txt ? 200 : 502, headers: { ...cors, 'Content-Type': 'application/json' } });
  }
};

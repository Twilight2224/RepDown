// Supabase Edge Function: secrets are stored with `supabase secrets set GEMINI_API_KEY=...`.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
Deno.serve(async (req) => {
  const auth = req.headers.get("Authorization"); if (!auth) return json({ error: "Authentication required" }, 401);
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, { global: { headers: { Authorization: auth } } });
  const { data: { user } } = await supabase.auth.getUser(); if (!user) return json({ error: "Authentication required" }, 401);
  const input = await req.json().catch(() => null) as { kind?: string; words?: string[] } | null;
  if (!input || !["card", "sentence", "idiom"].includes(input.kind || "") || !Array.isArray(input.words) || input.words.length < 1 || input.words.length > 10 || input.words.some(w => typeof w !== "string" || w.length > 80)) return json({ error: "Invalid bounded request" }, 400);
  const key = Deno.env.get("GEMINI_API_KEY"); if (!key) return json({ error: "Generation is not configured" }, 503);
  const model = Deno.env.get("GEMINI_MODEL") || "gemini-2.5-flash";
  const prompt = `Return JSON only. Create accurate German learner content for ${JSON.stringify(input.words)}. Required per item: german, english, hint, definition, exampleGerman1, exampleEnglish1, exampleGerman2, exampleEnglish2, article, plural, partOfSpeech, grammar. Do not invent facts.`;
  const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(key)}`, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }], generationConfig: { responseMimeType: "application/json", maxOutputTokens: 4096 } }) });
  if (!response.ok) return json({ error: "Provider unavailable", providerStatus: response.status }, response.status === 429 ? 429 : 502);
  const provider = await response.json(); const text = provider?.candidates?.[0]?.content?.parts?.[0]?.text;
  try { const items = JSON.parse(text); if (!Array.isArray(items) || items.length !== input.words.length) throw Error(); return json({ items }); } catch { return json({ error: "Provider returned invalid structured content" }, 502); }
});

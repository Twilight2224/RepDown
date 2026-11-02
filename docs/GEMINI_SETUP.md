# Optional Gemini generation proxy

Create a Gemini API key in the owner’s Google AI Studio account only after confirming the model/quota is free for the intended use. Never put this key in Android or browser code.

Set it only in Supabase Edge Function secrets:

```text
supabase secrets set GEMINI_API_KEY=... GEMINI_MODEL=gemini-2.5-flash
supabase functions deploy generate
```

The function authenticates the caller, caps a request at ten short words, uses JSON output, and rejects malformed responses. Generated German remains machine-generated and must be editable and reviewed by the learner. The app must not claim linguistic perfection.

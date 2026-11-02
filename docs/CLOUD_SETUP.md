# Optional Supabase setup

1. Create a free Supabase project and enable email authentication.
2. Install the Supabase CLI and run `supabase link`, then `supabase db push` from this project to apply `supabase/migrations/001_initial.sql`.
3. Verify the four row-level-security policies in the Supabase dashboard before entering any public URL/key in a client build.
4. Deploy the Edge Function only after following [GEMINI_SETUP.md](GEMINI_SETUP.md).

The migration stores encrypted-in-transit JSON records owned by `auth.uid()`. The service-role key must never be placed in an APK, browser build, or repository. Public anonymous keys are only safe with RLS enabled.

The current Android source keeps cloud networking disabled by default and has a validated-Wi-Fi gate. A production sync adapter still needs to be connected to the optional service boundary; local data is unaffected when a project is paused, out of quota, or offline.

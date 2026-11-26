# CESI OpenAI AI Gateway (Astra)

This is a server-side gateway scaffold for CESI's conversational AI. It uses OpenAI Responses API with **gpt-6-astra**. It is not connected to a ChatGPT consumer session or ChatGPT subscription.

## Security model
- OpenAI key stays on the Supabase Edge Function, never in Android code, Git, APK, or logs.
- Requires a valid Supabase Auth user token. Enable anonymous sign-in for the first CESI prototype, or use a supported account login flow.
- Rate-limits each authenticated user to 20 requests/minute using a database RPC.
- Keeps conversation history client-side for now; the function does not write prompt or answer content into the database.
- This gateway is conversational only. It does not execute Android actions. CESI's local capability/security/confirmation pipeline must remain the only device-action executor.
- Provider errors are deliberately generic to clients.

## Deployment checklist
1. Create a dedicated Supabase project for CESI. Do **not** reuse HanyaGo or Ramatpoly.
2. Enable Supabase Auth anonymous sign-in (or configure a real sign-in provider).
3. Apply `supabase/migrations/202611260001_cesi_ai_rate_limit.sql`.
4. Deploy `supabase/functions/cesi-ai-gateway/index.ts`.
5. Set Edge Function secrets: `OPENAI_API_KEY`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`. Supabase provides `SUPABASE_URL` to Edge Functions. Do not put these secrets in this repo.
6. Android client must sign in (anonymous Auth for prototype), store the session token securely, call the Edge Function with `Authorization: Bearer <user access token>`, and handle 401/429/502/503. Do not call OpenAI directly from the APK.
7. Add consent screen, AI-on/off, delete conversation, usage/cost cap, and offline fallback before public preview.
8. Test prompt injection, oversized input, expired token, rate limit, no internet, provider errors, and mixed Hausa/English before release.

## Model and cost note
Astra is API-billed separately from ChatGPT subscription allowances. Use the current official API pricing and configure spending limits before testing.

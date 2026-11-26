# CESI OpenAI AI Gateway (Astra)

This server-side gateway connects CESI's conversational brain to OpenAI Responses API using **gpt-6-astra**. It is not connected to the ChatGPT consumer app or subscription.

## Security
- OpenAI key stays on Supabase Edge Function, never Android code, Git, APK, or logs.
- Requires valid Supabase Auth token; client-supplied user IDs are not trusted.
- Database RPC enforces **20 requests/minute and 100 requests/user/day** (prototype safeguards, not a complete financial budget).
- Conversation history is sent from the client and is not persisted by this function.
- Provider errors are generic to clients; request timeout is bounded.
- The gateway is conversational only. Android actions must remain local through CESI's capability registry, risk/confirmation gate, ActionRouter, and verification.

## Setup
1. Create a **separate Supabase project for CESI**. Do not reuse HanyaGo or Ramatpoly.
2. Enable Supabase Auth anonymous sign-in for the prototype (or configure a supported login).
3. Apply migration `supabase/migrations/202611260001_cesi_ai_rate_limit.sql`.
4. Deploy Edge Function `supabase/functions/cesi-ai-gateway/index.ts`.
5. Set Edge Function secrets `OPENAI_API_KEY`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`. Supabase supplies `SUPABASE_URL`.
6. Android: authenticate via Supabase Auth, store session securely, call the gateway with `Authorization: Bearer <user access token>`, and handle 401/429/502/503/504.
7. Add an AI consent toggle, delete-history action, usage display, cost cap, and offline fallback before public preview.
8. Tests: invalid/expired token, rate limits, prompt injection, oversized input, no internet, provider errors, and Hausa-English mixed requests.

## History contract
The client should send only prior conversation turns in `history`; the current user message goes in `prompt`. Keep only the most recent 12 turns, each bounded to 4,000 characters. Avoid sending device identifiers, notification contents, contacts, or private screen text unless a specific future feature requires it and the user explicitly consents.

## Cost
API billing is separate from ChatGPT subscriptions. Configure a billing/spend limit at the provider account and monitor usage before testing. A per-user request cap alone is not a global spending cap.

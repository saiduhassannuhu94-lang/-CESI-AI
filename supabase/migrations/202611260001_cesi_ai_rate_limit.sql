create table if not exists public.cesi_ai_rate_limits (
  user_id uuid primary key,
  window_started_at timestamptz not null default now(),
  request_count integer not null default 1 check (request_count >= 0)
);

alter table public.cesi_ai_rate_limits enable row level security;
revoke all on table public.cesi_ai_rate_limits from public, anon, authenticated;

create or replace function public.cesi_ai_claim_request(p_user_id uuid, p_limit integer default 20)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  current_count integer;
begin
  insert into public.cesi_ai_rate_limits(user_id, window_started_at, request_count)
  values (p_user_id, now(), 1)
  on conflict (user_id) do update
    set window_started_at = case
          when cesi_ai_rate_limits.window_started_at <= now() - interval '1 minute'
            then now()
          else cesi_ai_rate_limits.window_started_at
        end,
        request_count = case
          when cesi_ai_rate_limits.window_started_at <= now() - interval '1 minute'
            then 1
          else cesi_ai_rate_limits.request_count + 1
        end
    where cesi_ai_rate_limits.window_started_at <= now() - interval '1 minute'
       or cesi_ai_rate_limits.request_count < greatest(p_limit, 1)
  returning request_count into current_count;

  return current_count is not null;
end;
$$;

revoke execute on function public.cesi_ai_claim_request(uuid, integer) from public, anon, authenticated;
grant execute on function public.cesi_ai_claim_request(uuid, integer) to service_role;

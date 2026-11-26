create table if not exists public.cesi_ai_rate_limits (
  user_id uuid primary key,
  minute_started_at timestamptz not null default now(),
  minute_count integer not null default 1 check (minute_count >= 0),
  day_key date not null default current_date,
  day_count integer not null default 1 check (day_count >= 0)
);

alter table public.cesi_ai_rate_limits enable row level security;
revoke all on table public.cesi_ai_rate_limits from public, anon, authenticated;

create or replace function public.cesi_ai_claim_request(
  p_user_id uuid,
  p_limit integer default 20,
  p_daily_limit integer default 100
)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  claimed_user_id uuid;
begin
  insert into public.cesi_ai_rate_limits(user_id, minute_started_at, minute_count, day_key, day_count)
  values (p_user_id, now(), 1, current_date, 1)
  on conflict (user_id) do update
    set minute_started_at = case
          when cesi_ai_rate_limits.minute_started_at <= now() - interval '1 minute'
            then now()
          else cesi_ai_rate_limits.minute_started_at
        end,
        minute_count = case
          when cesi_ai_rate_limits.minute_started_at <= now() - interval '1 minute'
            then 1
          else cesi_ai_rate_limits.minute_count + 1
        end,
        day_key = case
          when cesi_ai_rate_limits.day_key < current_date then current_date
          else cesi_ai_rate_limits.day_key
        end,
        day_count = case
          when cesi_ai_rate_limits.day_key < current_date then 1
          else cesi_ai_rate_limits.day_count + 1
        end
    where (cesi_ai_rate_limits.minute_started_at <= now() - interval '1 minute'
          or cesi_ai_rate_limits.minute_count < greatest(p_limit, 1))
      and (cesi_ai_rate_limits.day_key < current_date
          or cesi_ai_rate_limits.day_count < greatest(p_daily_limit, 1))
  returning user_id into claimed_user_id;

  return claimed_user_id is not null;
end;
$$;

revoke execute on function public.cesi_ai_claim_request(uuid, integer, integer) from public, anon, authenticated;
grant execute on function public.cesi_ai_claim_request(uuid, integer, integer) to service_role;

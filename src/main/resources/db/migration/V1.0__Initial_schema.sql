CREATE TYPE event_type AS ENUM (
  'Orgasm',
  'Edge',
  'Ruin',
  'Subsmash',
  'NotCute',
  'Pleading'
);

CREATE TABLE users (
  id              uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  user_discord_id bigint                                             NOT NULL,
  created_at      timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  updated_at      timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
);

CREATE TABLE events (
  id         uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  user_id    uuid                                               NOT NULL REFERENCES users (id),
  event      event_type                                         NOT NULL,
  amount     int                                                NOT NULL,
  created_at timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  updated_at timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
);

CREATE TABLE notifications (
  id           uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  from_user_id uuid                                               NOT NULL REFERENCES users (id),
  to_user_id   uuid                                               NOT NULL REFERENCES users (id),
  event        event_type                                         NOT NULL,
  accepted     boolean                                            NOT NULL DEFAULT FALSE,
  created_at   timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  updated_at   timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
);
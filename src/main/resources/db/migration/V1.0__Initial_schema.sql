CREATE TABLE users (
  id              uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  user_discord_id bigint                                             NOT NULL,
  created_at      timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  updated_at      timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
);

CREATE TABLE events (
  id               uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  receiver_user_id uuid                                               NOT NULL REFERENCES users (id),
  issuer_user_id   uuid REFERENCES users (id),
  event            text                                               NOT NULL,
  amount           int                                                NOT NULL,
  created_at       timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  updated_at       timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
);

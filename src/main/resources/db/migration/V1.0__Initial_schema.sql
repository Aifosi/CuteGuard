CREATE TABLE users (
  id                uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  user_discord_id   bigint                                             NOT NULL,
  guild_discord_id  bigint                                             NOT NULL,
  points            bigint                                             NOT NULL,
  points_today      bigint                                             NOT NULL,
  last_point_update timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  created_at        timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  updated_at        timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  UNIQUE (user_discord_id, guild_discord_id)
);


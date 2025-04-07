CREATE TABLE preferences (
  id               uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  user_id          uuid                                               NOT NULL REFERENCES users(id),
  pleading_opt_out boolean                                            NOT NULL,
  subsmash_opt_out boolean                                            NOT NULL,
  not_cute_opt_out boolean                                            NOT NULL,
  created_at       timestamp with time zone DEFAULT NOW()             NOT NULL,
  updated_at       timestamp with time zone DEFAULT NOW()             NOT NULL
);

CREATE INDEX idx_user_id ON preferences(user_id);
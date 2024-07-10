CREATE TABLE points (
  id           uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  user_id      uuid                                               NOT NULL REFERENCES users (id),
  points       int                                                NOT NULL,
  max_points   int                                                NOT NULL,
  last_message timestamp WITH TIME ZONE,
  created_at   timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  updated_at   timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
);
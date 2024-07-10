CREATE TABLE notifications (
  id           uuid                     DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
  from_user_id uuid                                               NOT NULL REFERENCES users (id),
  to_user_id   uuid                                               NOT NULL REFERENCES users (id),
  event        text                                               NOT NULL,
  accepted     boolean                                            NOT NULL DEFAULT FALSE,
  created_at   timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
  updated_at   timestamp WITH TIME ZONE DEFAULT NOW()             NOT NULL,
);

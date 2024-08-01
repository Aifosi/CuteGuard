ALTER TABLE events ADD COLUMN "date" timestamp WITH TIME ZONE DEFAULT now();

UPDATE events SET "date" = created_at;

ALTER TABLE events ALTER COLUMN "date" SET NOT NULL;

CREATE INDEX idx_user_discord_id ON users(user_discord_id);
CREATE INDEX idx_receiver_user_id ON events(receiver_user_id);
CREATE INDEX idx_action ON events("action");
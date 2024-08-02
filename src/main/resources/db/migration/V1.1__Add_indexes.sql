CREATE INDEX idx_user_discord_id ON users(user_discord_id);
CREATE INDEX idx_receiver_user_id ON events(receiver_user_id);
CREATE INDEX idx_action ON events("action");
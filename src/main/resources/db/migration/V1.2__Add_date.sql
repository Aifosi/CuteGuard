ALTER TABLE events ADD COLUMN "date" timestamp WITH TIME ZONE DEFAULT now();

UPDATE events SET "date" = created_at;

ALTER TABLE events ALTER COLUMN "date" SET NOT NULL;
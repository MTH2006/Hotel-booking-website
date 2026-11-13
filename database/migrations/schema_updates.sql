
-- ==================================================
-- HOTEL BOOKING WEBSITE
-- DATABASE SCHEMA UPDATES
-- ==================================================

-- Account management: Lock / Unlock users
ALTER TABLE users
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Keep existing room rows queryable after newer room availability fields were added.
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS status VARCHAR(30);
ALTER TABLE rooms ALTER COLUMN status SET DEFAULT 'AVAILABLE';
UPDATE rooms SET status = 'AVAILABLE' WHERE status IS NULL;
ALTER TABLE rooms ALTER COLUMN status SET NOT NULL;

ALTER TABLE rooms ADD COLUMN IF NOT EXISTS is_active BOOLEAN;
ALTER TABLE rooms ALTER COLUMN is_active SET DEFAULT TRUE;
UPDATE rooms SET is_active = TRUE WHERE is_active IS NULL;
ALTER TABLE rooms ALTER COLUMN is_active SET NOT NULL;

-- Let customers hide cancelled bookings from their own history without erasing admin records.
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS hidden_from_user BOOLEAN;
ALTER TABLE bookings ALTER COLUMN hidden_from_user SET DEFAULT FALSE;
UPDATE bookings SET hidden_from_user = FALSE WHERE hidden_from_user IS NULL;
ALTER TABLE bookings ALTER COLUMN hidden_from_user SET NOT NULL;

-- Store requested room quantity so availability checks count rooms, not booking records.
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS quantity INTEGER;
UPDATE bookings SET quantity = 1 WHERE quantity IS NULL OR quantity < 1;
ALTER TABLE bookings ALTER COLUMN quantity SET DEFAULT 1;
ALTER TABLE bookings ALTER COLUMN quantity SET NOT NULL;

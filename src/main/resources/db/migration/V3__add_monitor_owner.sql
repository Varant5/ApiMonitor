-- Every monitor belongs to exactly one user.
ALTER TABLE monitor ADD COLUMN user_id BIGINT;

-- Monitors created before user accounts existed have no owner and nobody could access them anymore.
-- This project has no production data yet, so they are removed (their checks go with them via ON DELETE CASCADE).
-- With real data you would instead assign them to an existing account, e.g. an admin.
DELETE FROM monitor WHERE user_id IS NULL;

ALTER TABLE monitor ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE monitor
    ADD CONSTRAINT fk_monitor_user
        FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE;

-- "list my monitors" and "find my monitor by id" both filter by user_id
CREATE INDEX idx_monitor_user_id ON monitor (user_id);

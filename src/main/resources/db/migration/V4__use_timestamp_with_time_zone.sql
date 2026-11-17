-- Store points in time unambiguously (java.time.Instant <-> TIMESTAMP WITH TIME ZONE).
--
-- Before this migration timestamps were saved as local time without a zone, so the same moment was
-- stored differently depending on the server's time zone (UTC in Docker, Europe/Warsaw on a laptop).
--
-- PostgreSQL converts the existing values using the time zone of the database session that runs this
-- migration. The JDBC driver sets that session to the JVM's default zone, so old rows are interpreted
-- in the zone of the machine running the app — i.e. the zone they were most likely written in.

ALTER TABLE monitor
    ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE monitor_check
    ALTER COLUMN checked_at TYPE TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE app_user
    ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE;

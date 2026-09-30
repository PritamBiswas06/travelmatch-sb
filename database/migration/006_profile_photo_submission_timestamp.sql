-- Accurate submission time for profile-photo moderation queue.
-- application.properties uses spring.jpa.hibernate.ddl-auto=update, which also
-- creates this nullable column automatically. Apply this SQL only if the column
-- is not already present in the production database.
ALTER TABLE trust_verification
  ADD COLUMN profile_photo_submitted_at DATETIME NULL;

-- Manual profile-photo review status. Safe for existing rows.
ALTER TABLE trust_verification
  ADD COLUMN profile_photo_review_status VARCHAR(24) NOT NULL DEFAULT 'NOT_SUBMITTED',
  ADD COLUMN profile_photo_reviewed_at DATETIME NULL,
  ADD COLUMN profile_photo_review_note VARCHAR(250) NULL;

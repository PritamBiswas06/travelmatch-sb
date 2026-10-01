-- Optional manual selfie review; upload alone never sets an identity/liveness badge.
ALTER TABLE trust_verification
  ADD COLUMN selfie_photo LONGBLOB NULL,
  ADD COLUMN selfie_content_type VARCHAR(40) NULL,
  ADD COLUMN selfie_review_status VARCHAR(24) NOT NULL DEFAULT 'NOT_SUBMITTED',
  ADD COLUMN selfie_submitted_at DATETIME NULL,
  ADD COLUMN selfie_reviewed_at DATETIME NULL,
  ADD COLUMN selfie_review_note VARCHAR(250) NULL;

-- TravelMatch safety checklist, owned by the creator of each travel plan.
CREATE TABLE IF NOT EXISTS trip_safety_checklist (
  id BIGINT NOT NULL AUTO_INCREMENT,
  travel_plan_id BIGINT NOT NULL,
  public_meeting_point BOOLEAN NOT NULL DEFAULT FALSE,
  itinerary_shared BOOLEAN NOT NULL DEFAULT FALSE,
  emergency_contact_informed BOOLEAN NOT NULL DEFAULT FALSE,
  transport_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  accommodation_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  offline_maps_ready BOOLEAN NOT NULL DEFAULT FALSE,
  check_in_plan_agreed BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_safety_checklist_plan (travel_plan_id),
  CONSTRAINT fk_safety_checklist_plan FOREIGN KEY (travel_plan_id) REFERENCES travel_plan(id) ON DELETE CASCADE
);

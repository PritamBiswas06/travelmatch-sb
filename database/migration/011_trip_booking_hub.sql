-- TripMatch Booking Hub: non-destructive tables for user-managed trip planning.
CREATE TABLE IF NOT EXISTS trip_booking_options (
  id BIGINT NOT NULL AUTO_INCREMENT,
  travel_plan_id BIGINT NOT NULL,
  created_by_id BIGINT NOT NULL,
  category VARCHAR(24) NOT NULL,
  provider_name VARCHAR(160) NOT NULL,
  booking_url VARCHAR(1000) NULL,
  estimated_price DECIMAL(12,2) NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'INR',
  travelers_covered INT NULL,
  start_at DATETIME NULL,
  end_at DATETIME NULL,
  notes VARCHAR(1500) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'CONSIDERING',
  preferred BIT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_booking_option_trip (travel_plan_id),
  KEY idx_booking_option_trip_category (travel_plan_id, category),
  CONSTRAINT fk_booking_option_plan FOREIGN KEY (travel_plan_id) REFERENCES travel_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_booking_option_creator FOREIGN KEY (created_by_id) REFERENCES user(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS trip_expenses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  travel_plan_id BIGINT NOT NULL,
  paid_by_id BIGINT NOT NULL,
  description VARCHAR(160) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'INR',
  category VARCHAR(24) NOT NULL DEFAULT 'OTHER',
  expense_date DATE NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_trip_expense_trip (travel_plan_id),
  CONSTRAINT fk_trip_expense_plan FOREIGN KEY (travel_plan_id) REFERENCES travel_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_trip_expense_payer FOREIGN KEY (paid_by_id) REFERENCES user(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS trip_checklist_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  travel_plan_id BIGINT NOT NULL,
  created_by_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  completed BIT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_trip_checklist_plan (travel_plan_id),
  CONSTRAINT fk_trip_checklist_plan FOREIGN KEY (travel_plan_id) REFERENCES travel_plan(id) ON DELETE CASCADE,
  CONSTRAINT fk_trip_checklist_creator FOREIGN KEY (created_by_id) REFERENCES user(id)
) ENGINE=InnoDB;

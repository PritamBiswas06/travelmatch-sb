-- Run once on an existing TravelMatch database if Hibernate ddl-auto=update is not being used.
ALTER TABLE travel_plan ADD COLUMN group_type VARCHAR(30) NULL;
ALTER TABLE travel_plan ADD COLUMN current_group_size INT NULL;
ALTER TABLE travel_plan ADD COLUMN min_group_size INT NULL;
ALTER TABLE travel_plan ADD COLUMN max_group_size INT NULL;
ALTER TABLE travel_plan ADD COLUMN looking_for VARCHAR(30) NULL;
ALTER TABLE travel_plan ADD COLUMN open_for_joining BIT(1) NULL;
ALTER TABLE travel_plan ADD COLUMN family_friendly BIT(1) NULL;
ALTER TABLE travel_plan ADD COLUMN children_allowed BIT(1) NULL;

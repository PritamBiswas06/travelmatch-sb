-- Travel DNA spectrum values. Existing users remain NULL until they configure them.
ALTER TABLE user
    ADD COLUMN dna_adventure_relaxation INT NULL,
    ADD COLUMN dna_budget_luxury INT NULL,
    ADD COLUMN dna_sunrise_nightlife INT NULL,
    ADD COLUMN dna_trekking_sightseeing INT NULL,
    ADD COLUMN dna_food_culture INT NULL,
    ADD COLUMN dna_planned_spontaneous INT NULL,
    ADD COLUMN dna_solo_group INT NULL,
    ADD COLUMN dna_nature_city INT NULL,
    ADD COLUMN dna_photography_activities INT NULL,
    ADD COLUMN dna_fast_slow INT NULL;

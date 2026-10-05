-- Kolumna app w modulach AI byla natywnym MySQL ENUM (lub waskim VARCHAR), przez co dodanie
-- nowego kanalu (GENERATOR_GRAFIKI) wywolywalo "Data truncated/too long". Zamiana na VARCHAR(32)
-- sprawia, ze kolejne kanaly nie wymagaja juz zmian w bazie. MODIFY zachowuje istniejace wartosci.
ALTER TABLE ai_conversation MODIFY COLUMN app VARCHAR(32) NOT NULL;
ALTER TABLE ai_usage MODIFY COLUMN app VARCHAR(32) NOT NULL;

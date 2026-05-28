-- V9:
-- 1) Nowa kolumna leads.source_url — pełny URL strony z której przyszedł lead.
-- 2) Sprzątanie po wczesnej wersji PublicLeadController (rekordy z opisem 'Auto-utworzone%').
-- 3) Dodanie sensownych marketingowych źródeł (GOOGLE_ADS, FACEBOOK_ADS, LINKEDIN_ADS) idempotentnie.
--    Od V9 PublicLeadController NIE tworzy auto-rekordów — używa tylko tych z bazy + fallback WEBSITE_CONTACT.

-- 1. Kolumna URL źródłowego
ALTER TABLE leads
    ADD COLUMN source_url VARCHAR(500) NULL;

ALTER TABLE leads_aud
    ADD COLUMN source_url VARCHAR(500) NULL;

-- 2. Sprzątanie auto-created rekordów (jeśli były z wczesnej wersji)
-- Najpierw przepisz leady które na nie wskazują na WEBSITE_CONTACT (bezpieczny default).
UPDATE leads
SET lead_source_id = (SELECT id FROM lead_source WHERE name = 'WEBSITE_CONTACT')
WHERE lead_source_id IN (
    SELECT id FROM (
        SELECT id FROM lead_source
        WHERE description LIKE 'Auto-utworzone%'
           OR description = 'Auto-utworzone przez PublicLeadController'
    ) t
);

DELETE FROM lead_source
WHERE description LIKE 'Auto-utworzone%'
   OR description = 'Auto-utworzone przez PublicLeadController';

-- 3. Standardowe kanały marketingowe (idempotentne — INSERT IGNORE na unique-name)
--    Jeśli kolumna name nie ma unikalnego constraintu, używamy WHERE NOT EXISTS.
INSERT INTO lead_source (name, description)
SELECT 'GOOGLE_ADS', 'Google Ads'
WHERE NOT EXISTS (SELECT 1 FROM lead_source WHERE name = 'GOOGLE_ADS');

INSERT INTO lead_source (name, description)
SELECT 'FACEBOOK_ADS', 'Facebook Ads'
WHERE NOT EXISTS (SELECT 1 FROM lead_source WHERE name = 'FACEBOOK_ADS');

INSERT INTO lead_source (name, description)
SELECT 'LINKEDIN_ADS', 'LinkedIn Ads'
WHERE NOT EXISTS (SELECT 1 FROM lead_source WHERE name = 'LINKEDIN_ADS');

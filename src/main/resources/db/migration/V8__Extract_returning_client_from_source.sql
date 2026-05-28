-- V8: "Klient powracający" przestaje być źródłem leada, staje się jego atrybutem (boolean).
--
-- Krok 1: nowa kolumna w głównej tabeli leads (default FALSE — bezpieczne dla istniejących wierszy).
-- Krok 2: nowa kolumna w tabeli audytu (Envers) — nullable, bo stare rewizje nie znały tego pola.
-- Krok 3: backfill — leady które historycznie miały lead_source = RETURNING_CLIENT dostają flagę TRUE.
-- Krok 4: tym leadom przepisujemy lead_source na OTHER, bo "klient powracający" to nie kanał pozyskania.
--          Audit (leads_aud) zachowa poprzedni stan — nic się nie traci.
-- Krok 5: oznaczamy rekord lead_source.RETURNING_CLIENT jako legacy (zostaje w bazie dla FK z aud-ów,
--          ale UI go ukryje przy doborze źródła dla nowych leadów).

-- 1. Pole w leads
ALTER TABLE leads
    ADD COLUMN is_returning_client BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Pole w leads_aud (Envers). Nullable — historia sprzed V8 ma NULL.
ALTER TABLE leads_aud
    ADD COLUMN is_returning_client BOOLEAN NULL;

-- 3. Backfill flagi
UPDATE leads
SET is_returning_client = TRUE
WHERE lead_source_id = (SELECT id FROM lead_source WHERE name = 'RETURNING_CLIENT');

-- 4. Przepisz źródło tych leadów na OTHER (musi istnieć — jest w base data; gdyby nie, NULL nie zaszkodzi).
UPDATE leads
SET lead_source_id = (SELECT id FROM lead_source WHERE name = 'OTHER')
WHERE lead_source_id = (SELECT id FROM lead_source WHERE name = 'RETURNING_CLIENT')
  AND EXISTS (SELECT 1 FROM lead_source WHERE name = 'OTHER');

-- 5. Oznacz legacy w opisie (rekord zostaje — może być wskazany przez stare rewizje audytu).
UPDATE lead_source
SET description = CONCAT('[LEGACY od V8 — przeniesione do leads.is_returning_client] ', COALESCE(description, ''))
WHERE name = 'RETURNING_CLIENT'
  AND description NOT LIKE '[LEGACY%';

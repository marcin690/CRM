-- Atrybucja marketingowa leadów: gclid + UTM jako OSOBNE kolumny (pod raporty i BigQuery).
-- Dotychczas parametry trafiały tylko do `description` (tekst) i `source_url` — nieużywalne
-- w hurtowni (nie da się JOIN po gclid ani GROUP BY po kampanii). Kolumny nullable, addytywne.

ALTER TABLE leads     ADD COLUMN gclid        VARCHAR(512) NULL;
ALTER TABLE leads     ADD COLUMN utm_source   VARCHAR(255) NULL;
ALTER TABLE leads     ADD COLUMN utm_medium   VARCHAR(255) NULL;
ALTER TABLE leads     ADD COLUMN utm_campaign VARCHAR(255) NULL;
ALTER TABLE leads     ADD COLUMN utm_term     VARCHAR(255) NULL;
ALTER TABLE leads     ADD COLUMN utm_content  VARCHAR(255) NULL;

-- Tabela audytowa Envers (spójnie z V9 dla source_url).
ALTER TABLE leads_aud ADD COLUMN gclid        VARCHAR(512) NULL;
ALTER TABLE leads_aud ADD COLUMN utm_source   VARCHAR(255) NULL;
ALTER TABLE leads_aud ADD COLUMN utm_medium   VARCHAR(255) NULL;
ALTER TABLE leads_aud ADD COLUMN utm_campaign VARCHAR(255) NULL;
ALTER TABLE leads_aud ADD COLUMN utm_term     VARCHAR(255) NULL;
ALTER TABLE leads_aud ADD COLUMN utm_content  VARCHAR(255) NULL;

-- Pod łączenie z Google Ads / BigQuery po gclid i grupowanie po kampanii.
CREATE INDEX idx_leads_gclid        ON leads (gclid);
CREATE INDEX idx_leads_utm_campaign ON leads (utm_campaign);

ALTER TABLE offers ADD COLUMN sales_team_id BIGINT;

-- Dodanie klucza obcego do tabeli sales_teams
ALTER TABLE offers ADD CONSTRAINT fk_sales_team_offer FOREIGN KEY (sales_team_id) REFERENCES sales_teams(id);

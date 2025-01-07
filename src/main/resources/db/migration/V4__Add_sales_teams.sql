-- Tworzenie tabeli sales_teams
CREATE TABLE sales_teams (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             name VARCHAR(255) NOT NULL UNIQUE
);

-- Dodanie kolumny sales_team_id do tabeli users
ALTER TABLE users ADD COLUMN sales_team_id BIGINT;

-- Dodanie klucza obcego pomiędzy users a sales_teams
ALTER TABLE users ADD CONSTRAINT fk_sales_team_user FOREIGN KEY (sales_team_id) REFERENCES sales_teams(id);

-- Dodanie przykładowych teamów sprzedażowych
INSERT INTO sales_teams (name) VALUES ('Team A'), ('Team B'), ('Team C');

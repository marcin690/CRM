ALTER TABLE project ADD COLUMN sales_team_id BIGINT;
ALTER TABLE project ADD CONSTRAINT fk_sales_team_project FOREIGN KEY (sales_team_id) REFERENCES sales_teams(id);

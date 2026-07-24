-- project.id musi byc AUTO_INCREMENT — encja Project uzywa @GeneratedValue(IDENTITY),
-- wiec Hibernate nie wysyla id w insercie i liczy, ze baza je nada.
--
-- Na tym prodzie tabela 'project' istniala wczesniej z kolumna id BEZ auto_increment,
-- przez co "insert into project (...)" konczyl sie: Field 'id' doesn't have a default value.
-- ddl-auto=update NIE dodaje auto_increment do istniejacej kolumny — trzeba migracja.
--
-- Defensywnie (Flyway leci przed ddl-auto): rusz kolumne tylko gdy tabela istnieje
-- i id nie jest juz auto_increment. Na swiezej bazie Hibernate stworzy id poprawnie -> NO-OP.

SET @has_project := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project');
SET @id_is_autoinc := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project'
      AND COLUMN_NAME = 'id' AND EXTRA LIKE '%auto_increment%');

SET @sql := IF(@has_project > 0 AND @id_is_autoinc = 0,
    'ALTER TABLE project MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

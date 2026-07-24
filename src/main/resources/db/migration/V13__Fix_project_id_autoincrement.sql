-- Kompleksowa, BEZPIECZNA naprawa AUTO_INCREMENT na kolumnach 'id' (PRIMARY KEY).
--
-- Kontekst: prod zaimportowany z dumpa bez flag AUTO_INCREMENT; encje uzywaja
-- @GeneratedValue(IDENTITY), wiec bez auto_increment insert konczy sie
-- "Field 'id' doesn't have a default value". ddl-auto=update tego nie naprawia.
--
-- Naprawiamy TYLKO tabele, gdzie to jest bezpieczne:
--   * 'id' jest JEDYNA kolumna PRIMARY KEY (pomija tabele audytowe Envers _AUD z PK zlozonym),
--   * tabela NIE ma juz zadnej innej kolumny auto_increment (MySQL: tylko jedna auto kolumna),
--   * typ int/bigint, jeszcze bez auto_increment.
-- Dodatkowo kazdy ALTER jest w bloku z CONTINUE HANDLER — jedna nietypowa tabela
-- nie przerywa calej migracji. FK chwilowo wylaczone (id bywa celem FK, np. comments->project).
-- Idempotentna: gdy nic nie brakuje => NO-OP.

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS fix_autoincrement_ids;

DELIMITER $$
CREATE PROCEDURE fix_autoincrement_ids()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE t_name VARCHAR(255);
    DECLARE c_type VARCHAR(255);
    DECLARE cur CURSOR FOR
        SELECT c.TABLE_NAME, c.COLUMN_TYPE
        FROM information_schema.COLUMNS c
        WHERE c.TABLE_SCHEMA = DATABASE()
          AND c.COLUMN_NAME = 'id'
          AND c.DATA_TYPE IN ('bigint', 'int')
          AND c.EXTRA NOT LIKE '%auto_increment%'
          -- id musi byc JEDYNA kolumna PRIMARY KEY tej tabeli
          AND (
              SELECT COUNT(*)
              FROM information_schema.KEY_COLUMN_USAGE k
              JOIN information_schema.TABLE_CONSTRAINTS tc
                ON tc.TABLE_SCHEMA = k.TABLE_SCHEMA AND tc.TABLE_NAME = k.TABLE_NAME
               AND tc.CONSTRAINT_NAME = k.CONSTRAINT_NAME
              WHERE k.TABLE_SCHEMA = c.TABLE_SCHEMA AND k.TABLE_NAME = c.TABLE_NAME
                AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
          ) = 1
          -- ...i ta jedyna kolumna PK to wlasnie 'id'
          AND EXISTS (
              SELECT 1
              FROM information_schema.KEY_COLUMN_USAGE k2
              JOIN information_schema.TABLE_CONSTRAINTS tc2
                ON tc2.TABLE_SCHEMA = k2.TABLE_SCHEMA AND tc2.TABLE_NAME = k2.TABLE_NAME
               AND tc2.CONSTRAINT_NAME = k2.CONSTRAINT_NAME
              WHERE k2.TABLE_SCHEMA = c.TABLE_SCHEMA AND k2.TABLE_NAME = c.TABLE_NAME
                AND k2.COLUMN_NAME = 'id' AND tc2.CONSTRAINT_TYPE = 'PRIMARY KEY'
          )
          -- tabela nie moze miec juz innej kolumny auto_increment
          AND NOT EXISTS (
              SELECT 1 FROM information_schema.COLUMNS c2
              WHERE c2.TABLE_SCHEMA = c.TABLE_SCHEMA AND c2.TABLE_NAME = c.TABLE_NAME
                AND c2.EXTRA LIKE '%auto_increment%'
          );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO t_name, c_type;
        IF done THEN
            LEAVE read_loop;
        END IF;
        -- Izolowana obsluga bledu: jedna nietypowa tabela nie przerywa migracji.
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
            SET @ddl = CONCAT('ALTER TABLE `', t_name, '` MODIFY `id` ', c_type, ' NOT NULL AUTO_INCREMENT');
            PREPARE st FROM @ddl;
            EXECUTE st;
            DEALLOCATE PREPARE st;
        END;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL fix_autoincrement_ids();
DROP PROCEDURE fix_autoincrement_ids;

SET FOREIGN_KEY_CHECKS = 1;

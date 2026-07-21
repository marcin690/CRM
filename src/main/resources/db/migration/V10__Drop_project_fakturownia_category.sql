-- V10:
-- Usuwa kolumnę `fakturownia_category` z tabeli `project` (i `project_aud`).
-- Zastępujemy ją relacją wiele-do-wielu przez tabele `fakturownia_account` +
-- `project_fakturownia_binding`. Pozwala pinować do jednego projektu wiele kont
-- z różnymi firmami (companyId 1/2) i rolami (REVENUE/COST).
--
-- Uwaga: pole `Client.fakturowniaCategory` (kolumna w tabeli `clients`)
-- ZOSTAJE — to inny use case (domyślna kategoria klienta) i jest poza zakresem
-- tej migracji.
--
-- Defensywnie: kolumny mogą nie istnieć na niektórych środowiskach (gdy
-- ddl-auto=update nigdy ich nie utworzył) — używamy procedury sprawdzającej
-- information_schema, by migracja była idempotentna.

DROP PROCEDURE IF EXISTS drop_column_if_exists;

DELIMITER //
CREATE PROCEDURE drop_column_if_exists(IN tbl VARCHAR(64), IN col VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tbl
          AND COLUMN_NAME = col
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tbl, ' DROP COLUMN ', col);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL drop_column_if_exists('project', 'fakturownia_category');
CALL drop_column_if_exists('project_aud', 'fakturownia_category');

DROP PROCEDURE IF EXISTS drop_column_if_exists;

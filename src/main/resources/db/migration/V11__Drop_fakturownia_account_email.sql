-- V11:
-- Pole `email` w `fakturownia_account` jest bezużyteczne — Fakturownia uwierzytelnia się
-- przez subdomenę + api_token, nie przez email. Plus okazało się, że każde "konto"
-- w Fakturowni to osobna subdomena z własnym tokenem (model „1 email = wiele firm"
-- nie istnieje w tym setupie), więc email nigdzie nie pasuje.
--
-- Idempotentnie: DROP tylko jeśli kolumna istnieje.

DROP PROCEDURE IF EXISTS drop_column_if_exists_v11;

DELIMITER //
CREATE PROCEDURE drop_column_if_exists_v11(IN tbl VARCHAR(64), IN col VARCHAR(64))
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

CALL drop_column_if_exists_v11('fakturownia_account', 'email');

DROP PROCEDURE IF EXISTS drop_column_if_exists_v11;

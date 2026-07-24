-- Kompleksowa naprawa AUTO_INCREMENT na kolumnach 'id' (PRIMARY KEY).
--
-- Kontekst: prod zostal zaimportowany z dumpa, ktory pogubil flagi AUTO_INCREMENT na
-- czesci starych tabel. Encje uzywaja @GeneratedValue(IDENTITY), wiec Hibernate nie
-- wysyla 'id' w insercie i baza musi je nadawac. Bez auto_increment kazdy insert konczyl
-- sie "Field 'id' doesn't have a default value". ddl-auto=update tego nie naprawia.
--
-- Zamiast latac tabela-po-tabeli, wykrywamy i naprawiamy WSZYSTKIE dotkniete tabele naraz:
-- kazda tabela w tej bazie, ktorej kolumna 'id' jest kluczem glownym typu int/bigint i NIE
-- ma jeszcze auto_increment. FK sa chwilowo wylaczone (kolumna 'id' bywa celem kluczy obcych,
-- np. comments -> project), a typ kolumny zachowujemy 1:1 (COLUMN_TYPE).
--
-- Migracja jest idempotentna: gdy nic nie brakuje (swiezy prod tworzony przez Hibernate) => NO-OP.

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
        JOIN information_schema.KEY_COLUMN_USAGE k
          ON k.TABLE_SCHEMA = c.TABLE_SCHEMA AND k.TABLE_NAME = c.TABLE_NAME AND k.COLUMN_NAME = c.COLUMN_NAME
        JOIN information_schema.TABLE_CONSTRAINTS tc
          ON tc.TABLE_SCHEMA = k.TABLE_SCHEMA AND tc.TABLE_NAME = k.TABLE_NAME AND tc.CONSTRAINT_NAME = k.CONSTRAINT_NAME
        WHERE c.TABLE_SCHEMA = DATABASE()
          AND c.COLUMN_NAME = 'id'
          AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
          AND c.DATA_TYPE IN ('bigint', 'int')
          AND c.EXTRA NOT LIKE '%auto_increment%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO t_name, c_type;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET @ddl = CONCAT('ALTER TABLE `', t_name, '` MODIFY `id` ', c_type, ' NOT NULL AUTO_INCREMENT');
        PREPARE st FROM @ddl;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL fix_autoincrement_ids();
DROP PROCEDURE fix_autoincrement_ids;

SET FOREIGN_KEY_CHECKS = 1;

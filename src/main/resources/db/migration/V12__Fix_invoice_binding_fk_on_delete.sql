-- Naprawa reguły usuwania dla invoice.binding_id.
-- Cel: usunięcie wiązania Fakturowni ma ODPIĄĆ faktury (SET NULL), a nie wysypać się na
-- naruszenie klucza obcego ani kasować historii faktur. Encja ma @OnDelete(SET_NULL), ale
-- Hibernate ddl-auto=update NIE zmienia reguły ON DELETE istniejącego FK — dlatego robimy to migracją.
--
-- Migracja jest odporna na losową nazwę FK generowaną przez Hibernate: najpierw znajdujemy i
-- usuwamy istniejący FK na kolumnie binding_id (jeśli jest), potem dodajemy nazwany z SET NULL.

SET @fk := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'invoice'
      AND COLUMN_NAME = 'binding_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @sql := IF(@fk IS NOT NULL, CONCAT('ALTER TABLE invoice DROP FOREIGN KEY ', @fk), 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_binding
        FOREIGN KEY (binding_id) REFERENCES project_fakturownia_binding (id)
        ON DELETE SET NULL;

-- Naprawa reguly usuwania dla invoice.binding_id: usuniecie wiazania ma ODPIAC faktury
-- (SET NULL), nie wysypac sie na FK ani kasowac historii.
--
-- WAZNE: Flyway odpala sie PRZED Hibernate ddl-auto. Przy pierwszym deployu tabele
-- 'invoice' / 'project_fakturownia_binding' moga jeszcze NIE istniec — wtedy migracja
-- musi byc NO-OP (Hibernate stworzy tabele z poprawnym @OnDelete(SET_NULL) chwile pozniej).
-- Ta migracja realnie naprawia FK tylko na ISTNIEJACYCH bazach ze zlym kluczem.
--
-- Kazda sciezka NO-OP uzywa 'SELECT 1' (na pewno da sie PREPARE), nie 'DO'.

-- Czy tabele/kolumna w ogole istnieja?
SET @has_invoice := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice');
SET @has_binding := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_fakturownia_binding');
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice' AND COLUMN_NAME = 'binding_id');

-- 1) Usun istniejacy FK na invoice.binding_id (jesli jest) — odporne na losowa nazwe.
SET @fk := (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoice'
      AND COLUMN_NAME = 'binding_id' AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1);
SET @drop_sql := IF(@fk IS NOT NULL, CONCAT('ALTER TABLE invoice DROP FOREIGN KEY ', @fk), 'SELECT 1');
PREPARE s1 FROM @drop_sql; EXECUTE s1; DEALLOCATE PREPARE s1;

-- 2) Dodaj FK z ON DELETE SET NULL — tylko gdy wszystko istnieje; inaczej NO-OP.
SET @add_sql := IF(@has_invoice > 0 AND @has_binding > 0 AND @has_col > 0,
    'ALTER TABLE invoice ADD CONSTRAINT fk_invoice_binding FOREIGN KEY (binding_id) REFERENCES project_fakturownia_binding (id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE s2 FROM @add_sql; EXECUTE s2; DEALLOCATE PREPARE s2;

-- Numer wersji dokumentu w obrębie oferty (WH/{oferta}/{wersja} w PDF). Istniejące numerujemy po kolei.
ALTER TABLE offer_document ADD COLUMN version INT NULL;

UPDATE offer_document d
JOIN (SELECT id, ROW_NUMBER() OVER (PARTITION BY offer_id ORDER BY id) AS rn FROM offer_document) x ON x.id = d.id
SET d.version = x.rn;

ALTER TABLE offer_document ADD UNIQUE KEY uq_offer_document_version (offer_id, version);

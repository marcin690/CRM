-- Automatyczna oferta PDF: dokumenty przy ofercie + pula FAQ do wyboru przez handlowca.

CREATE TABLE offer_document (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    offer_id           BIGINT       NOT NULL,
    client_type        VARCHAR(16)  NOT NULL,
    content            LONGTEXT     NULL,
    item_groups        TEXT         NULL,
    pdf_url            VARCHAR(255) NULL,
    generated_at       DATETIME(6)  NULL,
    status             VARCHAR(16)  NOT NULL DEFAULT 'READY',
    error              VARCHAR(1000) NULL,
    created_by         VARCHAR(255) NULL,
    creation_date      TIMESTAMP    NULL,
    last_modified_by   VARCHAR(255) NULL,
    last_modified_date TIMESTAMP    NULL,
    client_global_id   VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_offer_document_offer (offer_id),
    CONSTRAINT fk_offer_document_offer FOREIGN KEY (offer_id) REFERENCES offers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE offer_faq (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    question VARCHAR(500) NOT NULL,
    answer   TEXT         NOT NULL,
    position INT          NOT NULL DEFAULT 0,
    active   BIT(1)       NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Moduł Asystent AI (czaty Claude/ChatGPT/Gemini przez Dify).
-- CRM trzyma tylko WŁASNOŚĆ rozmów i ZUŻYCIE — treść wiadomości jest w Dify.
-- Izolacja: każdy odczyt/zapis filtruje po owner_user_id / user_id.

CREATE TABLE ai_conversation (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    owner_user_id        BIGINT       NOT NULL,
    app                  VARCHAR(16)  NOT NULL,
    dify_conversation_id VARCHAR(64)  NULL,
    title                VARCHAR(255) NULL,
    last_task_id         VARCHAR(64)  NULL,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    deleted_at           DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_conversation_dify (dify_conversation_id),
    KEY idx_ai_conversation_owner (owner_user_id, app, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_usage (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    user_email        VARCHAR(255)  NULL,
    conversation_id   BIGINT        NOT NULL,
    app               VARCHAR(16)   NOT NULL,
    model             VARCHAR(64)   NULL,
    dify_message_id   VARCHAR(64)   NOT NULL,
    prompt_tokens     INT           NOT NULL DEFAULT 0,
    completion_tokens INT           NOT NULL DEFAULT 0,
    cost_usd          DECIMAL(12,6) NULL,
    dify_total_price  DECIMAL(12,6) NULL,
    latency_ms        INT           NULL,
    created_at        DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_usage_message (dify_message_id),
    KEY idx_ai_usage_user_date (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

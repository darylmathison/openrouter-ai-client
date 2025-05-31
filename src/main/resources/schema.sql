DROP TABLE IF EXISTS attachments;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS chats;
DROP TABLE IF EXISTS saved_prompts;
DROP TABLE IF EXISTS external_tools;

CREATE TABLE chats
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    total_tokens   BIGINT,
    estimated_cost DOUBLE,
    model_used     VARCHAR(100)
);

CREATE TABLE messages
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id    BIGINT      NOT NULL,
    content    TEXT        NOT NULL,
    role       VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    tokens     INTEGER,
    FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE
);

CREATE TABLE attachments
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id     BIGINT       NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    file_path      VARCHAR(500) NOT NULL,
    file_type      VARCHAR(100),
    file_size      BIGINT,
    content_type   VARCHAR(100),
    is_processed   BOOLEAN DEFAULT FALSE,
    extracted_text TEXT,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE
);

CREATE TABLE saved_prompts
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    prompt         TEXT         NOT NULL,
    description    TEXT,
    system_message TEXT,
    model_name     VARCHAR(100),
    max_tokens     INTEGER,
    temperature    DOUBLE,
    category       VARCHAR(100),
    usage_count    BIGINT  DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    is_favorite    BOOLEAN DEFAULT FALSE,
    tags           VARCHAR(500)
);

CREATE TABLE external_tools
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    base_url      VARCHAR(500),
    api_key       VARCHAR(255),
    is_enabled    BOOLEAN DEFAULT TRUE,
    tool_type     VARCHAR(50),
    configuration TEXT,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    last_used_at  TIMESTAMP,
    usage_count   BIGINT  DEFAULT 0
);

-- Indexes for better performance
CREATE INDEX idx_messages_chat_id ON messages (chat_id);
CREATE INDEX idx_chats_updated_at ON chats (updated_at);
CREATE INDEX idx_chats_created_at ON chats (created_at);
CREATE INDEX idx_attachments_message_id ON attachments (message_id);
CREATE INDEX idx_saved_prompts_usage_count ON saved_prompts (usage_count);
CREATE INDEX idx_saved_prompts_category ON saved_prompts (category);
CREATE INDEX idx_saved_prompts_model_name ON saved_prompts (model_name);
CREATE INDEX idx_external_tools_enabled ON external_tools (is_enabled);
CREATE INDEX idx_external_tools_type ON external_tools (tool_type);
CREATE TABLE link_tags (
    link_id             BIGINT NOT NULL,
    tag_name            VARCHAR(100) NOT NULL,
    PRIMARY KEY (link_id, tag_name),
    FOREIGN KEY (link_id) REFERENCES links (id)
);

CREATE INDEX idx_link_tags_on_tag_name ON link_tags (tag_name);
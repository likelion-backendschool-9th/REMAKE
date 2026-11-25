CREATE TABLE receipt_image_deletions (
    image_key    varchar PRIMARY KEY,
    requested_at timestamp NOT NULL
);

CREATE INDEX idx_receipt_image_deletions_requested_at ON receipt_image_deletions (requested_at);

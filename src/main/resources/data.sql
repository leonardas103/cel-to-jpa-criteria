-- Content table
CREATE TABLE IF NOT EXISTS content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP DEFAULT NOW(),
    name VARCHAR(255) NOT NULL
);

-- Metadata table
CREATE TABLE IF NOT EXISTS metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id BIGINT NOT NULL,
    datakey VARCHAR(255) NOT NULL,
    datavalue VARCHAR(255) NOT NULL,
    FOREIGN KEY (content_id) REFERENCES content(id)
);

-- Insert mock content (fixed syntax)
INSERT INTO content (created_at, name) VALUES
(CURRENT_TIMESTAMP() - INTERVAL '1' DAY, 'Document 1'),
(CURRENT_TIMESTAMP() - INTERVAL '2' DAY, 'Document 2'),
(CURRENT_TIMESTAMP() - INTERVAL '3' DAY, 'Document 3'),
(CURRENT_TIMESTAMP() - INTERVAL '4' DAY, 'Document 4'),
(CURRENT_TIMESTAMP() - INTERVAL '5' DAY, 'Document 5');

-- Insert mock metadata entries (fixed trailing comma)
INSERT INTO metadata (content_id, datakey, datavalue) VALUES
(1, 'author', 'Alice'),
(1, 'category', 'red'),
(1, 'category', 'green'),
(1, 'category', 'blue'),
(2, 'author', 'Alice'),
(2, 'category', 'redorange'),
(2, 'category', 'aqua'),
(2, 'category', 'teal'),
(3, 'author', 'Bob'),
(3, 'category', 'pinkred'),
(3, 'category', 'olive'),
(3, 'category', 'lime'),
(4, 'author', 'Bob'),
(4, 'category', 'white'),
(4, 'category', 'silver'),
(4, 'category', 'navyblue'),
(5, 'author', 'Charlie'),
(5, 'category', 'maroon'),
(5, 'category', 'green'),
(5, 'category', 'navy');
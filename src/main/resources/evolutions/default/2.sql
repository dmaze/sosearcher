-- Data related to fetching questions.

-- !Ups

CREATE TABLE post_types(
  id INTEGER PRIMARY KEY,
  post_type VARCHAR(16) NOT NULL UNIQUE
);
CREATE INDEX post_types_type ON post_types(post_type);
INSERT INTO post_types(post_type) VALUES ('question');

CREATE TABLE fetch_types(
  id INTEGER PRIMARY KEY,
  fetch_type VARCHAR(16) NOT NULL UNIQUE
);
CREATE INDEX fetch_types_type ON fetch_types(fetch_type);
INSERT INTO fetch_types(fetch_type) VALUES ('metadata');
INSERT INTO fetch_types(fetch_type) VALUES ('body');

CREATE TABLE fetches(
  id INTEGER PRIMARY KEY,
  post_type_id INTEGER NOT NULL REFERENCES post_types(id),
  site_id INTEGER REFERENCES sites(id),
  post_number INTEGER NOT NULL,
  fetch_type_id INTEGER NOT NULL REFERENCES fetch_types(id),
  timestamp DATETIME,
  result VARCHAR(255)
);
CREATE INDEX fetches_post ON fetches(site_id, post_number);

-- !Downs

DROP TABLE fetches;
DROP TABLE fetch_types;
DROP TABLE post_types;

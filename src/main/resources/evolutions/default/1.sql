-- !Ups

CREATE TABLE site(
  id INTEGER PRIMARY KEY,
  api_site_parameter VARCHAR(64) NOT NULL,
  audience VARCHAR(256) NOT NULL,
  icon_url VARCHAR(256) NOT NULL,
  logo_url VARCHAR(256) NOT NULL,
  name VARCHAR(256) NOT NULL,
  site_url VARCHAR(256) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX site_param ON site(api_site_parameter);

-- !Downs

DROP INDEX site_param;
DROP TABLE site;

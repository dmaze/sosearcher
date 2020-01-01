-- !Ups

CREATE TABLE sites(
  id INTEGER PRIMARY KEY,
  api_site_parameter VARCHAR(64) NOT NULL,
  audience VARCHAR(256) NOT NULL,
  icon_url VARCHAR(256) NOT NULL,
  logo_url VARCHAR(256) NOT NULL,
  name VARCHAR(256) NOT NULL,
  site_url VARCHAR(256) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX sites_param ON sites(api_site_parameter);

-- !Downs

DROP INDEX sites_param;
DROP TABLE sites;

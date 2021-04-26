-- liquibase formatted sql

-- changeset mdellamerlina:3 logicalFilePath:xtages-console.xml
CREATE TABLE "xtages_build_events"
(
	"id" SERIAL PRIMARY KEY,
	"time" 	TIMESTAMPTZ NOT NULL,
	"status" VARCHAR(255) NOT NULL,
	"operation" VARCHAR(255) NOT NULL,
	"user" INT NOT NULL,
	CONSTRAINT "fk_user" FOREIGN KEY("user") REFERENCES xtages_user("id")
)

-- changeset mdellamerlina:4 logicalFilePath:xtages-console.xml
CREATE TABLE "xtages_build"
(
	"project" 		VARCHAR(255) NOT NULL,
	"commit" 		VARCHAR(255) NOT NULL,
	"build_events" INT NOT NULL,
	PRIMARY KEY("project", "commit"),
	CONSTRAINT "fk_build_project" FOREIGN KEY("project") REFERENCES xtages_project("name"),
	CONSTRAINT "fk_build_events" FOREIGN KEY("build_events") REFERENCES  xtages_build_events("id")
)

-- liquibase formatted sql

-- changeset mdellamerlina:3 logicalFilePath:xtages-console.xml
CREATE TABLE "build_events"
(
	"id" SERIAL PRIMARY KEY,
	"time" 	TIMESTAMPTZ NOT NULL,
	"status" VARCHAR(255) NOT NULL,
	"operation" VARCHAR(255) NOT NULL,
	"user" INT NOT NULL,
	"environment" VARCHAR(255) NOT NULL,
	CONSTRAINT "build_events_xtages_user_id_fkey" FOREIGN KEY("user") REFERENCES xtages_user("id")
)

-- changeset mdellamerlina:4 logicalFilePath:xtages-console.xml
CREATE TABLE "build"
(
	"project" 		VARCHAR(255) NOT NULL,
	"commit" 		VARCHAR(255) NOT NULL,
	"build_events" INT NOT NULL,
	PRIMARY KEY("project", "commit"),
	CONSTRAINT "build_project_name_fkey" FOREIGN KEY("project") REFERENCES project("name"),
	CONSTRAINT "build_build_events_id_fkey" FOREIGN KEY("build_events") REFERENCES  build_events("id")
)

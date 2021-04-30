-- liquibase formatted sql

-- changeset mdellamerlina:5 logicalFilePath:xtages-console.xml
CREATE SEQUENCE "build_events_seq" AS BIGINT;

ALTER TABLE "build_events"
    ALTER COLUMN "id" SET DEFAULT nextval('build_events_seq');

ALTER TABLE "build_events"
    ADD COLUMN "project_id" INT,
    ADD COLUMN "commit" VARCHAR(255),
    DROP COLUMN "build_id",
    ALTER COLUMN "time" TYPE TIMESTAMP,
    ALTER COLUMN "id" TYPE BIGINT,
    DROP CONSTRAINT build_events_build_id_fkey,
    ADD CONSTRAINT "build_events_project_id_fkey" FOREIGN KEY("project_id") REFERENCES project("id");

UPDATE build_events SET "project_id" = 0, "commit" = '' , "time" = NOW();

ALTER TABLE "build_events" ALTER "project_id" SET NOT NULL;
ALTER TABLE "build_events" ALTER "commit" SET NOT NULL;
ALTER TABLE "build_events" ALTER "commit" SET NOT NULL;

DROP TABLE "build";

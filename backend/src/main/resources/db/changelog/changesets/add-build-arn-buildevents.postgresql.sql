-- liquibase formatted sql

-- changeset mdellamerlina:7 logicalFilePath:xtages-console.xml
ALTER TABLE "build_events"
    ADD COLUMN "build_arn" VARCHAR(2048);

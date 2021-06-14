-- liquibase formatted sql

-- changeset mdellamerlina:10 logicalFilePath:xtages-console.xml
ALTER TABLE "organization"
    ADD COLUMN hash VARCHAR(32);

ALTER TABLE "project"
    ADD COLUMN hash VARCHAR(32);

-- changeset mdellamerlina:11 logicalFilePath:xtages-console.xml

ALTER TABLE "organization"
    ALTER COLUMN "hash" SET NOT NULL;

ALTER TABLE "project"
    ALTER COLUMN "hash" SET NOT NULL;

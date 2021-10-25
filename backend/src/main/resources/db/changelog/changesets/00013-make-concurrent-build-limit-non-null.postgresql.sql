-- liquibase formatted sql

-- changeset mdellamerlina:68 logicalFilePath:xtages-console.xml
UPDATE "plan"
SET "concurrent_build_limit" = -1
WHERE "concurrent_build_limit" IS NULL;

ALTER TABLE "plan"
    ALTER COLUMN "concurrent_build_limit" SET NOT NULL;
ALTER TABLE "plan"
    ADD CONSTRAINT "concurrent_build_limit_valid" CHECK ("concurrent_build_limit" = -1 OR
                                                         ("concurrent_build_limit" BETWEEN 1 AND 2));

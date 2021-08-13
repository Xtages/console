-- liquibase formatted sql

-- changeset czuniga:50 logicalFilePath:xtages-console.xml
UPDATE "recipe"
SET "repository" = 'Xtages/recipes'
WHERE "repository" = 'Xtages/recipe';

-- changeset czuniga:51 logicalFilePath:xtages-console.xml
ALTER TABLE "recipe"
    ADD CONSTRAINT "valid_repository_check" CHECK ("repository" in ('Xtages/recipes'));
-- rollback: ALTER TABLE DROP CONSTRAINT "valid_repository_check";

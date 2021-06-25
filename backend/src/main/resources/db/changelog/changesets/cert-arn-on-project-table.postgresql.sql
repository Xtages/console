-- liquibase formatted sql

-- changeset czuniga:43 logicalFilePath:xtages-console.xml
ALTER TABLE "project"
    ADD COLUMN "cert_arn" VARCHAR(2048);
-- rollback ALTER TABLE "project" DROP COLUMN "cert_arn";

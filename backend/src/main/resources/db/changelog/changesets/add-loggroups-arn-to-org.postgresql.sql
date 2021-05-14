-- liquibase formatted sql

-- changeset czuniga:16 logicalFilePath:xtages-console.xml
ALTER TABLE "organization"
    ADD COLUMN "ci_log_group_arn" VARCHAR(2048),
    ADD COLUMN "cd_log_group_arn" VARCHAR(2048);


-- liquibase formatted sql

-- changeset czuniga:5 logicalFilePath:xtages-console.xml
ALTER TABLE "project"
    ALTER COLUMN "version" TYPE VARCHAR(255),
    ALTER COLUMN "organization" TYPE VARCHAR(255);

-- changeset czuniga:6 logicalFilePath:xtages-console.xml
ALTER TABLE "project"
    RENAME COLUMN "pass_check_rule_enable" TO "pass_check_rule_enabled";

-- changeset czuniga:7 logicalFilePath:xtages-console.xml
ALTER TABLE "project"
    DROP CONSTRAINT "project_organization_name_fkey";
ALTER TABLE "project"
    ADD CONSTRAINT "project_organization_name_fkey" FOREIGN KEY ("organization") REFERENCES "organization" ("name");


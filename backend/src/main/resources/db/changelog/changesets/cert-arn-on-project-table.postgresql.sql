-- liquibase formatted sql

-- changeset czuniga:43 logicalFilePath:xtages-console.xml
ALTER TABLE "project"
    ADD COLUMN "cert_arn" VARCHAR(2048);
-- rollback ALTER TABLE "project" DROP COLUMN "cert_arn";

-- changeset czuniga:44 logicalFilePath:xtages-console.xml
ALTER TABLE "project"
    ADD COLUMN "associated_domain" TEXT;
ALTER TABLE "project"
    ADD CONSTRAINT "cert_arn_and_associated_domain_consistent"
        CHECK ((ROW ("cert_arn", "associated_domain") IS NULL) OR
               (ROW ("cert_arn", "associated_domain") IS NOT NULL));
-- rollback ALTER TABLE "project" DROP COLUMN "associated_domain";
-- rollback ALTER TABLE "project" DROP CONSTRAINT "cert_arn_and_associated_domain_consistent";

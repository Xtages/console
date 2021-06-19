-- liquibase formatted sql

-- changeset czuniga:38 logicalFilePath:xtages-console.xml
ALTER TABLE "organization_to_plan"
    ADD COLUMN "start_time" TIMESTAMP NOT NULL,
    ADD COLUMN "end_time"   TIMESTAMP
;
-- rollback ALTER TABLE "organization_to_plan" DROP COLUMN "start_time";
-- rollback ALTER TABLE "organization_to_plan" DROP COLUMN "end_time";

-- changeset czuniga:40 logicalFilePath:xtages-console.xml
ALTER TABLE "plan"
    ALTER COLUMN "limit_projects" TYPE BIGINT,
    ALTER COLUMN "limit_monthly_build_minutes" TYPE BIGINT,
    ALTER COLUMN "limit_monthly_data_transfer_gbs" TYPE BIGINT;
ALTER TABLE "credit"
    ALTER COLUMN "amount" TYPE BIGINT;

-- changeset czuniga:41 logicalFilePath:xtages-console.xml
INSERT INTO "plan" (name, limit_projects, limit_monthly_build_minutes, limit_monthly_data_transfer_gbs)
VALUES ('Unlimited', -1, -1, -1);

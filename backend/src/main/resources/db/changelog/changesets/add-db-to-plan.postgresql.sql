-- liquibase formatted sql

-- changeset mdellamerlina:12 logicalFilePath:xtages-console.xml
ALTER TABLE "plan"
    ADD COLUMN "db_instance"    VARCHAR(256),
    ADD COLUMN "db_storage_gbs" BIGINT;
-- rollback ALTER TABLE "plan" DROP COLUMN "db_instance", DROP COLUMN "db_storage_gbs";


UPDATE "plan" SET "db_instance" = 'db.t2.micro', "db_storage_gbs" = 20;

ALTER TABLE "plan"
    ALTER COLUMN "db_instance"    SET NOT NULL,
    ALTER COLUMN "db_storage_gbs" SET NOT NULL;
-- rollback ALTER TABLE "plan" ALTER COLUMN "db_instance" SET NULL, ALTER COLUMN "db_storage_gbs" SET NULL;

-- changeset mdellamerlina:13 logicalFilePath:xtages-console.xml

ALTER TABLE "organization"
    ADD COLUMN "ssm_db_pass_path" VARCHAR(2048),
    ADD COLUMN "rds_endpoint"     VARCHAR(2048);
-- rollback ALTER TABLE "organization" DROP COLUMN "ssm_db_pass_path", DROP COLUMN "rds_endpoint";

-- changeset mdellamerlina:14 logicalFilePath:xtages-console.xml
ALTER TABLE "organization"
    ADD COLUMN "rds_arn" VARCHAR(2048);
-- rollback ALTER TABLE "organization" DROP COLUMN "rds_arn";

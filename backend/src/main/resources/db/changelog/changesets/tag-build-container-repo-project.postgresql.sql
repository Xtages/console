-- liquibase formatted sql

-- changeset mdellamerlina:9 logicalFilePath:xtages-console.xml

ALTER TABLE "organization"
    DROP COLUMN "ecr_repository_arn";

ALTER TABLE "project"
    ADD COLUMN "ecr_repository_arn" VARCHAR(2048);

ALTER TABLE "build"
    ADD COLUMN "tag" VARCHAR(512);

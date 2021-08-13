-- liquibase formatted sql
-- changeset mdellamerlina:19 logicalFilePath:xtages-console.xml

ALTER TABLE "project_deployment"
    RENAME COLUMN "start_time" TO "status_change_time";


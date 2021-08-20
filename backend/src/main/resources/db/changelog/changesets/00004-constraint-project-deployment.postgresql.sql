-- liquibase formatted sql
-- changeset mdellamerlina:22 logicalFilePath:xtages-console.xml

ALTER TABLE "project_deployment"
    ADD CONSTRAINT "build_id_status_unique" UNIQUE(status, build_id);

-- liquibase formatted sql
-- changeset mdellamerlina:16 logicalFilePath:xtages-console.xml

CREATE TYPE "deploy_status" AS ENUM
    ('PROVISIONING', 'DEPLOYED', 'DRAINING', 'DRAINED');

CREATE TABLE "project_deployment"
 (
     "id"            BIGSERIAL PRIMARY KEY,
     "project_id"    INT NOT NULL,
     "build_id"      BIGINT NOT NULL,
     "status"        "deploy_status",
     "start_time"    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
     CONSTRAINT "project_deployment_project_id_fkey" FOREIGN KEY("project_id") REFERENCES project("id"),
     CONSTRAINT "project_deployment_build_id_fkey" FOREIGN KEY("build_id") REFERENCES build("id")
 );

ALTER TABLE "project"
    ADD COLUMN "project_deployment_id" BIGINT,
    ADD CONSTRAINT "project_project_deployment_id_fkey" FOREIGN KEY("project_deployment_id") REFERENCES project_deployment("id");


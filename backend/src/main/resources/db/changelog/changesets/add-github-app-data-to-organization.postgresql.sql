-- liquibase formatted sql


-- changeset czuniga:4
CREATE TYPE "github_app_installation_status" AS ENUM
    ('ACTIVE', 'SUSPENDED', 'NEW_PERMISSIONS_REQUIRED');
ALTER TABLE "organization"
    ADD COLUMN "github_app_installation_id"     BIGINT UNIQUE,
    ADD COLUMN "github_app_installation_status" "github_app_installation_status",
    ADD CONSTRAINT "github_app_cols_consistent" CHECK ((ROW ("github_app_installation_id", "github_app_installation_status") IS NULL) OR
                                                       (ROW ("github_app_installation_id", "github_app_installation_status") IS NOT NULL));


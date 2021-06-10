-- liquibase formatted sql

-- changeset czuniga:26 logicalFilePath:xtages-console.xml
ALTER TABLE "recipe"
    ADD COLUMN "build_script_path"    TEXT,
    ADD COLUMN "deploy_script_path"   TEXT,
    ADD COLUMN "promote_script_path"  TEXT,
    ADD COLUMN "rollback_script_path" TEXT;

UPDATE "recipe"
SET "build_script_path"    = 'node/ci/build.sh',
    "deploy_script_path"   = 'node/cd/deploy.sh',
    "promote_script_path"  = 'node/cd/promote.sh',
    "rollback_script_path" = 'node/cd/rollback.sh'
WHERE "project_type" = 'NODE';

ALTER TABLE "recipe"
    ALTER COLUMN "build_script_path" SET NOT NULL,
    ALTER COLUMN "deploy_script_path" SET NOT NULL,
    ALTER COLUMN "promote_script_path" SET NOT NULL,
    ALTER COLUMN "rollback_script_path" SET NOT NULL;

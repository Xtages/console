-- liquibase formatted sql

-- changeset mdellamerlina:6 logicalFilePath:xtages-console.xml
ALTER TYPE project_type RENAME VALUE 'NODEJS' TO 'NODE';

UPDATE "project" SET "version" = '15.13.0' WHERE "version" = '15'

-- liquibase formatted sql

-- changeset mdellamerlina:29 logicalFilePath:xtages-console.xml
ALTER TABLE plan
    ADD COLUMN concurrent_build_limit INT;

UPDATE plan SET concurrent_build_limit=1 where name='Free'

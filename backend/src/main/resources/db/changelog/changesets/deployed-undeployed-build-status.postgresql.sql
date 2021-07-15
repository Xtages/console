-- liquibase formatted sql
-- changeset mdellamerlina:16 logicalFilePath:xtages-console.xml

ALTER TYPE "build_status" ADD VALUE 'DEPLOYED';
ALTER TYPE "build_status" ADD VALUE 'UNDEPLOYED';

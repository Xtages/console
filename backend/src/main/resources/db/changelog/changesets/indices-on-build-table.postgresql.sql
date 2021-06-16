-- liquibase formatted sql

-- changeset czuniga:39 logicalFilePath:xtages-console.xml
CREATE INDEX "build_start_time_idx" ON "build" USING "btree" ("start_time" DESC);
CREATE INDEX "build_end_time_idx" ON "build" USING "btree" ("end_time" DESC);
-- rollback DROP INDEX "build_start_time_idx";
-- rollback DROP INDEX "build_end_time_idx";

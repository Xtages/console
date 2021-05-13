-- liquibase formatted sql

-- changeset czuniga:12 logicalFilePath:xtages-console.xml
ALTER TABLE "build_events"
    ALTER COLUMN "time" TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE "build_events"
    RENAME COLUMN "time" TO "start_time";
ALTER TABLE "build_events"
    ADD COLUMN "end_time" TIMESTAMP WITH TIME ZONE;
ALTER TABLE "build_events"
    ADD COLUMN "name" VARCHAR(255);
ALTER TABLE "build_events"
    ADD COLUMN "message" text;
UPDATE "build_events"
SET "name"   = 'SENT_TO_BUILD',
    "status" = 'SUCCEEDED';
ALTER TABLE "build_events"
    ALTER COLUMN "name" SET NOT NULl;

-- changeset czuniga:13 logicalFilePath:xtages-console.xml
ALTER TABLE "build_events" RENAME TO "build_event";

-- changeset czuniga:14 logicalFilePath:xtages-console.xml
ALTER TABLE "build_event" ADD COLUMN "notification_id" VARCHAR(100);

-- changeset czuniga:15 logicalFilePath:xtages-console.xml
ALTER TABLE "build_event"
    ALTER COLUMN "start_time" TYPE TIMESTAMP WITHOUT TIME ZONE,
    ALTER COLUMN "end_time" TYPE TIMESTAMP WITHOUT TIME ZONE;


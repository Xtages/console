-- liquibase formatted sql

-- changeset czuniga:19 logicalFilePath:xtages-console.xml
CREATE TABLE "github_user"
(
    username   VARCHAR(50) PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    avatar_url TEXT
);

CREATE TYPE "build_type" AS ENUM ('CI', 'CD');

CREATE TABLE "build"
(
    id                     BIGSERIAL PRIMARY KEY,
    project_id             INTEGER                             NOT NULL,
    "user_id"              INTEGER,
    "github_user_username" VARCHAR(50),
    type                   "build_type"                        NOT NULL,
    environment            VARCHAR(255)                        NOT NULL,
    status                 VARCHAR(255)                        NOT NULL,
    start_time             TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    end_time               TIMESTAMP,
    commit                 VARCHAR(255)                        NOT NULL,
    build_arn              VARCHAR(2048) UNIQUE,
    CONSTRAINT "build_has_user" CHECK (("user_id" IS NOT NULL AND "github_user_username" IS NULL) OR
                                       ("user_id" IS NULL AND "github_user_username" IS NOT NULL)),
    CONSTRAINT "build_project_id_fkey" FOREIGN KEY ("project_id") REFERENCES project ("id"),
    CONSTRAINT "build_xtages_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES xtages_user ("id"),
    CONSTRAINT "build_github_user_username_fkey" FOREIGN KEY ("github_user_username") REFERENCES github_user ("username")
);

INSERT INTO "build" ("project_id", "user_id", "type", "environment", "status", "start_time", "end_time", "commit",
                     "build_arn")
SELECT "project_id",
       "user",
       "operation"::build_type,
       "environment",
       "status",
       "start_time",
       "end_time",
       "commit",
       "build_arn"
FROM "build_event"
WHERE "name" = 'SENT_TO_BUILD'
LIMIT 1;

ALTER TABLE "build_event"
    ADD COLUMN "build_id" BIGINT;

UPDATE "build_event"
SET "build_id" = "build"."id"
FROM "build"
WHERE "build"."build_arn" = "build_event"."build_arn";

ALTER TABLE "build_event"
    ALTER COLUMN "build_id" SET NOT NULL;

ALTER TABLE "build_event"
    ADD CONSTRAINT "build_even_build_id_fkey" FOREIGN KEY ("build_id") REFERENCES "build" ("id");

ALTER TABLE "build_event"
    DROP COLUMN "operation",
    DROP COLUMN "user",
    DROP COLUMN "environment",
    DROP COLUMN "project_id",
    DROP COLUMN "build_arn"
;

-- changeset czuniga:20 logicalFilePath:xtages-console.xml
ALTER TABLE "build_event"
    DROP COLUMN "commit";

-- changeset czuniga:21 logicalFilePath:xtages-console.xml
CREATE TYPE "build_status" AS ENUM ('NOT_PROVISIONED', 'SUCCEEDED', 'FAILED', 'IN_PROGRESS', 'UNKNOWN');
UPDATE "build"
SET "status" = 'SUCCEEDED'
WHERE "status" = 'STARTED';
ALTER TABLE "build"
    ALTER COLUMN "status" TYPE "build_status" USING "status"::build_status;

-- changeset czuniga:22 logicalFilePath:xtages-console.xml
ALTER TABLE "build"
    RENAME COLUMN "commit" TO "commit_hash";

-- changeset czuniga:23 logicalFilePath:xtages-console.xml
DELETE FROM "build_event" WHERE "name" = 'SENT_TO_BUILD';

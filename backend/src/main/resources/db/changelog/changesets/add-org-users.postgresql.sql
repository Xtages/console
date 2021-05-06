-- liquibase formatted sql

-- changeset czuniga:10 logicalFilePath:xtages-console.xml
ALTER TABLE "xtages_user"
    ADD COLUMN "organization_name" VARCHAR(255),
    ADD COLUMN "is_owner"          BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE "xtages_user"
SET "organization_name" = org.name,
    "is_owner"          = TRUE
FROM (SELECT "name", "owner_id"
      FROM "organization"
               JOIN "xtages_user" on "organization"."owner_id" = "xtages_user"."id") as "org"
WHERE "xtages_user"."id" = "org"."owner_id";

ALTER TABLE "organization"
    DROP COLUMN "owner_id";

ALTER TABLE "xtages_user"
    ALTER COLUMN "organization_name" SET NOT NULL,
    ALTER COLUMN "is_owner" SET NOT NULL;
ALTER TABLE "xtages_user"
    ADD CONSTRAINT "user_organization_name_fkey" FOREIGN KEY ("organization_name") REFERENCES "organization" ("name");

-- liquibase formatted sql

-- changeset czuniga:28 logicalFilePath:xtages-console.xml
-- Add a `build_id_seq` column to each `project` that will control the sequence of build ids
ALTER TABLE "project"
    ADD COLUMN "build_id_seq" BIGINT NOT NULL DEFAULT 0;
-- rollback ALTER TABLE "project" DROP COLUMN "build_id_seq";

-- changeset czuniga:29 logicalFilePath:xtages-console.xml
-- The `next_build_external_id` increments `project.build_id_seq` and returns the next `build_id`.
CREATE FUNCTION next_build_external_id(project_id INT) RETURNS BIGINT
    LANGUAGE 'sql'
AS
$BODY$
UPDATE "project"
SET "build_id_seq" = "build_id_seq" + 1
WHERE id = project_id
RETURNING "build_id_seq"
$BODY$;
-- rollback DROP FUNCTION "next_build_external_id";

-- changeset czuniga:30 logicalFilePath:xtages-console.xml
-- Populate old build rows
ALTER TABLE "build"
    ADD COLUMN "external_id" BIGINT;

UPDATE "build"
SET "external_id" = "ext_id"
FROM (SELECT "b"."id", next_build_external_id(project_id := "b"."project_id") AS ext_id
      FROM "build" AS "b"
      ORDER BY "start_time") AS u
WHERE "build"."id" = "u"."id";

ALTER TABLE "build"
    ALTER COLUMN "external_id" SET NOT NULL;
-- rollback ALTER TABLE "build" DROP COLUMN "external_id";

-- changeset czuniga:31 logicalFilePath:xtages-console.xml
-- `build.external_id` + `build.project_id` should be UNIQUE
ALTER TABLE "build"
    ADD CONSTRAINT "external_id_and_project_id_unique" UNIQUE ("external_id", "project_id");
-- rollback ALTER TABLE "build" DROP CONSTRAINT "external_id_and_project_id_unique";

-- changeset czuniga:32 logicalFilePath:xtages-console.xml
-- Use a trigger to populate `build.external_id` on `INSERT`.
CREATE FUNCTION fill_build_external_id() RETURNS TRIGGER
    LANGUAGE 'plpgsql'
AS
'
    BEGIN
    SELECT next_build_external_id(project_id := NEW.project_id)
    INTO NEW.external_id; RETURN NEW; END;
';

CREATE TRIGGER "fill_build_external_id_trigger"
    BEFORE INSERT
    ON "build"
    FOR EACH ROW
EXECUTE PROCEDURE "fill_build_external_id"();
-- rollback DROP TRIGGER "fill_build_external_id" ON "build";
-- rollback DROP FUNCTION "fill_build_external_id" ON "build";

-- changeset czuniga:33 logicalFilePath:xtages-console.xml
-- Rename `build.external_build_id` to `build.build_number`
ALTER TABLE "build" RENAME COLUMN "external_id" TO "build_number";
ALTER TRIGGER "fill_build_external_id_trigger" ON "build" RENAME TO "fill_build_number";
ALTER FUNCTION "fill_build_external_id"() RENAME TO "fill_build_number";
-- rollback ALTER TABLE "build" RENAME COLUMN "build_number" TO "external_id";
-- rollback ALTER TRIGGER "fill_build_number" ON "build" RENAME TO "fill_build_external_id_trigger";
-- rollback ALTER FUNCTION "fill_build_number"() RENAME TO "fill_build_external_id";


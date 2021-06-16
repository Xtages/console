-- liquibase formatted sql

-- changeset czuniga:37 logicalFilePath:xtages-console.xml
DROP TRIGGER "fill_build_number" ON "build";
DROP FUNCTION "fill_build_number"();

CREATE FUNCTION fill_build_number() RETURNS TRIGGER
    LANGUAGE 'plpgsql'
AS
'
    BEGIN
    SELECT next_build_external_id(project_id := NEW.project_id)
    INTO NEW.build_number;
    RETURN NEW;
    END;
';

CREATE TRIGGER "fill_build_number"
    BEFORE INSERT
    ON "build"
    FOR EACH ROW
EXECUTE PROCEDURE "fill_build_number"();

ALTER FUNCTION "next_build_external_id"(INT) RENAME TO "next_build_id";
ALTER TABLE "build" RENAME CONSTRAINT "external_id_and_project_id_unique" TO "build_id_and_project_id_unique";

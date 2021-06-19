-- liquibase formatted sql

-- changeset czuniga:42 logicalFilePath:xtages-console.xml
ALTER FUNCTION "next_build_id"(INT) RENAME TO "next_build_number";
ALTER TABLE "build" RENAME CONSTRAINT "build_id_and_project_id_unique" TO "build_number_and_project_id_unique";

CREATE OR REPLACE FUNCTION fill_build_number() RETURNS TRIGGER
    LANGUAGE 'plpgsql'
AS
'
    BEGIN
    SELECT next_build_number(project_id := NEW.project_id)
    INTO NEW.build_number;
    RETURN NEW;
    END;
';

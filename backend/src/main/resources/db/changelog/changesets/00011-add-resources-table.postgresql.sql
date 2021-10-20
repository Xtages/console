-- liquibase formatted sql

-- changeset czuniga:61 logicalFilePath:xtages-console.xml
ALTER TYPE "resource_type" RENAME VALUE 'MONTHLY_BUILD_MINUTES' TO 'BUILD_MINUTES';
ALTER TYPE "resource_type" RENAME VALUE 'MONTHLY_DATA_TRANSFER_GBS' TO 'DATA_TRANSFER';
ALTER TYPE "resource_type" RENAME VALUE 'DB_STORAGE_GBS' TO 'POSTGRESQL';


-- changeset czuniga:62 logicalFilePath:xtages-console.xml

-- A function that will make sure that:
--    * ON INSERT:
--        - `new.create_timestamp` defaults to `CURRENT_TIMESTAMP AT TIME ZONE "UTC"`
--        - `new.update_timestamp` defaults to `NULL`
--    * ON UPDATE:
--        - `new.create_timestamp` is not modified
--        - `new.update_timestamp` is updated to `CURRENT_TIMESTAMP AT TIME ZONE "UTC"`
CREATE OR REPLACE FUNCTION fill_audit_timestamps() RETURNS TRIGGER
    LANGUAGE 'plpgsql'
AS
'
    BEGIN
      -- THIS IS AN INSERT
      IF old IS NULL THEN
        IF new.create_timestamp IS NOT NULL THEN
          new.create_timestamp := CURRENT_TIMESTAMP AT TIME ZONE "UTC";
        END IF;
        new.update_timestamp := NULL;
      ELSE
        -- THIS IS AN UPDATE
        IF old.create_timestamp IS NOT NULL THEN
          new.create_timestamp := old.create_timestamp;
        END IF;
        new.update_timestamp := CURRENT_TIMESTAMP AT TIME ZONE "UTC";
      END IF;
      RETURN new;
    END;
';

CREATE TYPE "resource_status" AS ENUM ('REQUESTED', 'PROVISIONED', 'WAIT_LISTED');
CREATE TABLE "resource"
(
    id                BIGSERIAL PRIMARY KEY,
    organization_name VARCHAR(255)             NOT NULL,
    resource_type     resource_type            NOT NULL,
    resource_status   resource_status          NOT NULL,
    resource_arn      VARCHAR(2048),
    resource_endpoint VARCHAR(2048),
    create_timestamp  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    update_timestamp  TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY ("organization_name") REFERENCES "organization" ("name")
);
CREATE TRIGGER "fill_audit_timestamps"
    BEFORE INSERT OR UPDATE
    ON "resource"
    FOR EACH ROW
EXECUTE PROCEDURE "fill_audit_timestamps"();

-- changeset czuniga:63 logicalFilePath:xtages-console.xml

-- A function that will make sure that:
--    * ON INSERT:
--        - `new.create_timestamp` defaults to `CURRENT_TIMESTAMP AT TIME ZONE "UTC"`
--        - `new.update_timestamp` defaults to `NULL`
--    * ON UPDATE:
--        - `new.create_timestamp` is not modified
--        - `new.update_timestamp` is updated to `CURRENT_TIMESTAMP AT TIME ZONE "UTC"`
CREATE OR REPLACE FUNCTION fill_audit_timestamps() RETURNS TRIGGER
    LANGUAGE 'plpgsql'
AS
'
    BEGIN
    -- THIS IS AN INSERT
    IF old IS NULL THEN
        IF new.create_timestamp IS NOT NULL THEN
          new.create_timestamp := (CURRENT_TIMESTAMP AT TIME ZONE ''UTC'');
    END IF;
    new.update_timestamp := NULL;
    ELSE
        -- THIS IS AN UPDATE
        IF old.create_timestamp IS NOT NULL THEN
          new.create_timestamp := old.create_timestamp;
    END IF;
    new.update_timestamp := (CURRENT_TIMESTAMP AT TIME ZONE ''UTC'');
    END IF;
    RETURN new;
    END;
';

-- changeset czuniga:64 logicalFilePath:xtages-console.xml
INSERT INTO "resource"
(organization_name, resource_arn, resource_endpoint, resource_type, resource_status)
SELECT "name",
       "rds_arn",
       "rds_endpoint",
       'POSTGRESQL',
       CASE
           WHEN rds_endpoint IS NULL THEN 'REQUESTED'::resource_status
           WHEN rds_endpoint IS NOT NULL THEN 'PROVISIONED'::resource_status
           END
FROM "organization"
WHERE "rds_arn" IS NOT NULL;

-- changeset czuniga:65 logicalFilePath:xtages-console.xml
ALTER TABLE "organization"
    DROP COLUMN "rds_endpoint",
    DROP COLUMN "rds_arn";

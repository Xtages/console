-- liquibase formatted sql

-- changeset czuniga:66 logicalFilePath:xtages-console.xml
CREATE OR REPLACE VIEW "resource_allocation" AS
SELECT "resource_type", COUNT(*) as "count"
FROM "resource"
WHERE "resource_status" = 'REQUESTED'
   OR "resource_status" = 'PROVISIONED'
GROUP BY "resource_type";

CREATE TABLE "resource_limit"
(
    "resource_type" resource_type NOT NULL PRIMARY KEY,
    "limit" INT NOT NULL CHECK ("limit" >= 0)
);

INSERT INTO "resource_limit" ("resource_type", "limit") VALUES ('POSTGRESQL', 25);

-- changeset czuniga:67 logicalFilePath:xtages-console.xml
ALTER TABLE "resource" ADD CONSTRAINT "unique_resource_per_org" UNIQUE ("organization_name", "resource_type");

-- liquibase formatted sql

-- changeset czuniga:45 logicalFilePath:xtages-console.xml
ALTER TABLE "plan"
    ADD COLUMN "product_id" VARCHAR(255) UNIQUE;
-- rollback ALTER TABLE "plan" DROP COLUMN "product_id";

-- changeset czuniga:46 logicalFilePath:xtages-console.xml
UPDATE "plan"
SET "product_id" = 'INTERNAL_TESTING_' || "plan_row"."id"
FROM (
         SELECT "id"
         FROM "plan"
         ORDER BY "id"
     ) "plan_row"
WHERE "plan_row"."id" = "plan"."id";

-- changeset czuniga:47 logicalFilePath:xtages-console.xml
ALTER TABLE "plan"
    ALTER COLUMN "product_id" SET NOT NULL;

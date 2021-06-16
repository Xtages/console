-- liquibase formatted sql

-- changeset czuniga:34 logicalFilePath:xtages-console.xml
CREATE TABLE IF NOT EXISTS "plan"
(
    "id"                              SERIAL PRIMARY KEY,
    "name"                            TEXT NOT NULL,
    "limit_projects"                  INT  NOT NULL,
    "limit_monthly_build_minutes"     INT  NOT NULL,
    "limit_monthly_data_transfer_gbs" INT  NOT NULl
);
-- rollback DROP TABLE "plan";

-- changeset czuniga:35 logicalFilePath:xtages-console.xml
CREATE TABLE IF NOT EXISTS "organization_to_plan"
(
    "organization_name" VARCHAR(255) NOT NULL,
    "plan_id"           INTEGER      NOT NULL,
    CONSTRAINT "organization_to_plan_pkey" PRIMARY KEY ("organization_name", "plan_id"),
    FOREIGN KEY ("organization_name") REFERENCES "organization" ("name"),
    FOREIGN KEY ("plan_id") REFERENCES "plan" ("id")
);
-- rollback DROP TABLE "organization_to_plan";

-- changeset czuniga:36 logicalFilePath:xtages-console.xml
CREATE TYPE "credit_type" AS ENUM ('AWARD', 'ADD_ON');
CREATE TYPE "resource_type" AS ENUM ('PROJECT', 'MONTHLY_BUILD_MINUTES', 'MONTHLY_DATA_TRANSFER_GBS');
CREATE TABLE IF NOT EXISTS "credit"
(
    "id"                SERIAL PRIMARY KEY,
    "organization_name" VARCHAR(255)    NOT NULL,
    "type"              "credit_type"   NOT NULL,
    "resource"          "resource_type" NOT NULL,
    "amount"            INT             NOT NULL,
    "created_time"      TIMESTAMP       NOT NULL,
    "expiry_time"       TIMESTAMP,
    "created_by"        VARCHAR(255)    NOT NULL,
    "justification"     TEXT            NOT NULL,
    FOREIGN KEY ("organization_name") REFERENCES "organization" ("name")
);
-- rollback DROP TABLE "credit";
-- rollback DROP TYPE "resource_type";
-- rollback DROP TYPE "credit_type";

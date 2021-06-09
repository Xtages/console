-- liquibase formatted sql

-- changeset czuniga:25 logicalFilePath:xtages-console.xml
CREATE VIEW "build_stats_per_month" AS
SELECT count(*) AS "build_count", "organization", "status", date_trunc('month', "end_time")::date as "date"
FROM "build"
         JOIN "project" on "project"."id" = "build"."project_id"
         JOIN "organization" on "project"."organization" = "organization"."name"
GROUP BY "organization", "status", date_trunc('month', "end_time")::date;

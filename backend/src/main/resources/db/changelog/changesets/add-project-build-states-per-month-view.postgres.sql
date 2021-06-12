-- liquibase formatted sql

-- changeset czuniga:27 logicalFilePath:xtages-console.xml
DROP VIEW "build_stats_per_month";

CREATE VIEW "build_stats_per_month" AS
SELECT count(*)                                      AS "build_count",
       "project"."organization"                      AS "organization",
       "project"."name"                              AS "project",
       "build"."status"                              AS "status",
       date_trunc('month', "build"."end_time")::date as "date"
FROM "build"
         JOIN "project" on "project"."id" = "build"."project_id"
         JOIN "organization" on "project"."organization" = "organization"."name"
GROUP BY "project"."organization", "project"."name", "build"."status", date_trunc('month', "build"."end_time")::date;

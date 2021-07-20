-- liquibase formatted sql

-- changeset czuniga:49 logicalFilePath:xtages-console.xml
-- Starter annually
UPDATE "plan"
SET "product_id" = 'prod_Jsq4Qw9yNh0DQp'
WHERE "product_id" = 'prod_JlMOKm62S0nw4u';
-- Starter monthly
UPDATE "plan"
SET "product_id" = 'prod_Jsq3ZlbMcy5M6W'
WHERE "product_id" = 'prod_JlMSuNcAWvH93b';
-- Professional annually
UPDATE "plan"
SET "product_id" = 'prod_Jsq3ZpoC2kPnuz'
WHERE "product_id" = 'prod_JlMTyIPa78LDcn';
-- Professional monthly
UPDATE "plan"
SET "product_id" = 'prod_Jsq2fxkGO8IqLi'
WHERE "product_id" = 'prod_JlMUWhrUK2NTfc';

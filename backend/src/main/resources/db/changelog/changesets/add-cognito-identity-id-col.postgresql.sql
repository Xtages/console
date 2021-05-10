-- liquibase formatted sql

-- changeset czuniga:11 logicalFilePath:xtages-console.xml
ALTER TABLE "xtages_user"
    ADD COLUMN "cognito_identity_id" VARCHAR(255);

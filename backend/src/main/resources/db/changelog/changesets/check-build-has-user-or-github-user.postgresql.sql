-- liquibase formatted sql

-- changeset czuniga:48 logicalFilePath:xtages-console.xml
ALTER TABLE "build"
    ADD CONSTRAINT "check_build_has_user_id_or_github_user_username_is_set"
        CHECK ("user_id" IS NOT NULL OR "github_user_username" IS NOT NULL);

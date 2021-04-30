-- liquibase formatted sql

-- changeset czuniga:8 logicalFilePath:xtages-console.xml
ALTER TABLE "organization"
    ADD COLUMN "ecr_repository_arn" VARCHAR(2048);

-- changeset czuniga:9 logicalFilePath:xtages-console.xml
ALTER TABLE "project"
    ADD COLUMN "codebuild_ci_project_arn" VARCHAR(2048),
    ADD COLUMN "codebuild_cd_project_arn" VARCHAR(2048),
    ADD COLUMN "gh_repo_full_name" VARCHAR(255);

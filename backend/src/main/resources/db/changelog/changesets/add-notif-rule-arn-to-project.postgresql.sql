-- liquibase formatted sql

-- changeset czuniga:17 logicalFilePath:xtages-console.xml
ALTER TABLE "organization"
    ADD COLUMN "codebuild_ci_notification_rule_arn" VARCHAR(2048),
    ADD COLUMN "codebuild_cd_notification_rule_arn" VARCHAR(2048);

-- changeset czuniga:18 logicalFilePath:xtages-console.xml
ALTER TABLE "organization"
    DROP COLUMN "codebuild_ci_notification_rule_arn",
    DROP COLUMN "codebuild_cd_notification_rule_arn";
ALTER TABLE "project"
    ADD COLUMN "codebuild_ci_notification_rule_arn" VARCHAR(2048),
    ADD COLUMN "codebuild_cd_notification_rule_arn" VARCHAR(2048);

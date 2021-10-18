-- liquibase formatted sql

-- changeset czuniga:53 logicalFilePath:xtages-console.xml
ALTER TABLE "github_user"
    ADD COLUMN "authorization_token"                 TEXT,
    ADD COLUMN "authorization_token_expiration_date" TIMESTAMP,
    ADD COLUMN "refresh_token"                       TEXT,
    ADD COLUMN "refresh_token_expiration_date"       TIMESTAMP,
    ADD CONSTRAINT "authorization_token_has_expiration_date" CHECK (("authorization_token" IS NULL) =
                                                                    ("authorization_token_expiration_date" IS NULL)),
    ADD CONSTRAINT "refresh_token_has_expiration_date" CHECK (("refresh_token" IS NULL) =
                                                              ("refresh_token_expiration_date" IS NULL));

-- changeset czuniga:54 logicalFilePath:xtages-console.xml
ALTER TABLE "github_user"
    ADD COLUMN "github_id" BIGINT;

UPDATE "github_user"
SET "github_id" = 0
WHERE github_user."github_id" IS NULL;

ALTER TABLE "github_user"
    ALTER COLUMN "github_id" SET NOT NULL;

ALTER TABLE "github_user"
    RENAME COLUMN "authorization_token" TO "authorization_token_arn";
ALTER TABLE "github_user"
    RENAME COLUMN "refresh_token" TO "refresh_token_arn";

-- changeset czuniga:55 logicalFilePath:xtages-console.xml
ALTER TABLE "github_user"
    RENAME COLUMN "authorization_token_arn" TO "authorization_token_ssm_param";
ALTER TABLE "github_user"
    RENAME COLUMN "refresh_token_arn" TO "refresh_token_ssm_param";

-- changeset czuniga:56 logicalFilePath:xtages-console.xml
CREATE TYPE "github_organization_type" AS enum ('INDIVIDUAL', 'ORGANIZATION');
ALTER TABLE "organization"
    ADD COLUMN "github_organization_type" "github_organization_type";
UPDATE "organization"
SET "github_organization_type" = 'ORGANIZATION'
WHERE organization."github_organization_type" IS NULL;
ALTER TABLE "organization"
    ALTER COLUMN "github_organization_type" SET NOT NULL;

-- changeset czuniga:57 logicalFilePath:xtages-console.xml
ALTER TABLE "github_user"
    ADD COLUMN "oauth_token_ssm_param"       TEXT,
    ADD COLUMN "oauth_token_expiration_date" TIMESTAMP,
    ADD CONSTRAINT "oauth_token_has_expiration_date" CHECK (("oauth_token_ssm_param" IS NULL) =
                                                            ("oauth_token_expiration_date" IS NULL));

-- changeset czuniga:58 logicalFilePath:xtages-console.xml
ALTER TABLE "github_user"
    ALTER COLUMN "email" DROP NOT NULL;

-- changeset czuniga:59 logicalFilePath:xtages-console.xml
ALTER TABLE "github_user"
    ALTER COLUMN "name" DROP NOT NULL;

-- changeset czuniga:60 logicalFilePath:xtages-console.xml
ALTER TABLE "organization"
    ADD COLUMN "github_oauth_authorized" BOOLEAN;
UPDATE "organization"
SET "github_oauth_authorized" = FALSE
WHERE "github_organization_type" = 'INDIVIDUAL';
ALTER TABLE "organization"
    ADD CONSTRAINT "github_oauth_authorized_only_true_for_individuals" CHECK (("github_organization_type" =
                                                                               'ORGANIZATION' AND
                                                                               "github_oauth_authorized" IS NULL) OR
                                                                              ("github_organization_type" =
                                                                               'INDIVIDUAL' AND
                                                                               "github_oauth_authorized" IS NOT NULL));

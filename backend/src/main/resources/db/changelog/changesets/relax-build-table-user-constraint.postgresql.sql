-- liquibase formatted sql

-- changeset czuniga:24 logicalFilePath:xtages-console.xml

ALTER TABLE "build"
    DROP CONSTRAINT "build_has_user";

INSERT INTO "github_user" (username, email, name, avatar_url)
VALUES ('czuniga-xtages', 'czuniga@xtages.com', 'Carlos Zuniga',
        'https://avatars.githubusercontent.com/czuniga-xtages')
ON CONFLICT DO NOTHING;

UPDATE "build"
SET "github_user_username" = 'czuniga-xtages'
WHERE user_id = (SELECT "id" FROM "xtages_user" WHERE "cognito_user_id" = '5b2a09e0-ddd5-4450-bab1-f924725f247a');

ALTER TABLE "build"
    ALTER COLUMN "github_user_username" SET NOT NULL;

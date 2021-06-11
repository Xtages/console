-- changeset czuniga:27 logicalFilePath:xtages-console.xml

ALTER TABLE "build"
    ALTER COLUMN "github_user_username" DROP NOT NULL;

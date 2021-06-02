-- liquibase formatted sql

-- changeset mdellamerlina:8 logicalFilePath:xtages-console.xml
CREATE TABLE "recipe"
(
    "id"            SERIAL PRIMARY KEY,
    "project_type"   "project_type",
    "version"        VARCHAR(255) NOT NULL,
    "repository"     VARCHAR(255) NOT NULL,
    "tag"            VARCHAR(255) NOT NULL
);

INSERT INTO recipe( "project_type", "version", "repository", "tag")
	VALUES ('NODE', '15.13.0', 'Xtages/recipes', 'v0.1.0');

ALTER TABLE "project"
    DROP COLUMN "type",
    DROP COLUMN "version",
    ADD COLUMN "recipe" INT,
    ADD CONSTRAINT "project_recipe_id_fkey" FOREIGN KEY("recipe") REFERENCES recipe("id");

UPDATE project SET recipe = 1;

ALTER TABLE "project" ALTER "recipe" SET NOT NULL;



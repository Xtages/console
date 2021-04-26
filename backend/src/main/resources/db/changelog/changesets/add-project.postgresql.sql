-- liquibase formatted sql

-- changeset mdellamerlina:1 logicalFilePath:xtages-console.xml
CREATE TYPE "project_type" AS ENUM
    ('NODEJS');

-- changeset mdellamerlina:2 logicalFilePath:xtages-console.xml
CREATE TABLE "project"
(
	"name" 		VARCHAR(255) NOT NULL UNIQUE,
	"type" 		"project_type",
	"version" 	INT,
	"organization" INT NOT NULL,
	"user" INT NOT NULL,
	"pass_check_rule_enable" BOOLEAN NOT NULL DEFAULT FALSE,
	PRIMARY KEY("name", "organization"),
	CONSTRAINT "type_version_consistent" CHECK ((ROW ("type", "version") IS NULL) OR
                                                       (ROW ("type", "version") IS NOT NULL)),
	CONSTRAINT "project_organization_name_fkey" FOREIGN KEY("name") REFERENCES organization("name"),
	CONSTRAINT "project_xtages_user_id_fkey" FOREIGN KEY("user") REFERENCES xtages_user("id")
)

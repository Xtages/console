-- liquibase formatted sql

-- changeset czuniga:1 logicalFilePath:seed-xtages-console-dev-db.xml

-- Xtages
INSERT INTO
    "organization"(name, stripe_customer_id, subscription_status, github_app_installation_id,
    github_app_installation_status, ecr_repository_arn)
	VALUES
	('Xtages', 'cus_JNlr07Ilq8tnxc', 'ACTIVE', 16569285,
	'ACTIVE', 'arn:aws:ecr:us-east-1:606626603369:repository/xtages');

-- czuniga@xtages.com
INSERT INTO "xtages_user"
    (id, cognito_user_id, organization_name, is_owner, cognito_identity_id)
VALUES (1, '5b2a09e0-ddd5-4450-bab1-f924725f247a', 'Xtages', true, 'us-east-1:6831d19d-ff68-453e-95a8-41ee218bf754');

SELECT pg_catalog.setval('public.xtages_user_id_seq', 1, true);

INSERT INTO "project"
    (id, name, type, version, organization, "user", pass_check_rule_enabled,
    codebuild_ci_project_arn, codebuild_cd_project_arn, gh_repo_full_name)
	VALUES
	(1, 'test2', 'NODE', '15.13.0', 'Xtages', 1, false,
	'arn:aws:codebuild:us-east-1:606626603369:project/xtages_test2_ci',
    'arn:aws:codebuild:us-east-1:606626603369:project/xtages_test2_cd', 'Xtages/test');

SELECT pg_catalog.setval('public.project_id_seq', 1, true);

INSERT INTO "stripe_checkout_session"
    (organization_name, stripe_checkout_session_id)
    VALUES
    ('Xtages', 'cs_test_a1d6zBhwGHiJWtj6YhMvLiXqLybR5qTtNO8Pf5HVasQrieGf0H6QavPQB8');

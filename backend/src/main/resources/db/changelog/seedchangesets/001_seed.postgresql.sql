-- liquibase formatted sql

-- changeset czuniga:1 logicalFilePath:seed-xtages-console-dev-db.xml

-- Xtages
INSERT INTO
    "organization"(name, stripe_customer_id, subscription_status, github_app_installation_id,
    github_app_installation_status, ecr_repository_arn, ci_log_group_arn, cd_log_group_arn)
	VALUES
	('Xtages', 'cus_JNlr07Ilq8tnxc', 'ACTIVE', 16569285,
	'ACTIVE', 'arn:aws:ecr:us-east-1:606626603369:repository/xtages',
	'arn:aws:logs:us-east-1:606626603369:log-group:xtages_ci_logs',
	'arn:aws:logs:us-east-1:606626603369:log-group:xtages_cd_logs');

-- czuniga@xtages.com
INSERT INTO "xtages_user"
    (id, cognito_user_id, organization_name, is_owner, cognito_identity_id)
VALUES (1, '5b2a09e0-ddd5-4450-bab1-f924725f247a', 'Xtages', true, 'us-east-1:6831d19d-ff68-453e-95a8-41ee218bf754');

SELECT pg_catalog.setval('public.xtages_user_id_seq', 1, true);

SELECT pg_catalog.setval('public.project_id_seq', 1, true);

INSERT INTO "stripe_checkout_session"
    (organization_name, stripe_checkout_session_id)
    VALUES
    ('Xtages', 'cs_test_a1d6zBhwGHiJWtj6YhMvLiXqLybR5qTtNO8Pf5HVasQrieGf0H6QavPQB8');

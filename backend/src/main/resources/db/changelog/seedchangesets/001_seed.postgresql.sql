-- liquibase formatted sql

-- changeset czuniga:1 logicalFilePath:seed-xtages-console-dev-db.xml

-- Xtages
INSERT INTO "organization"(name, stripe_customer_id, subscription_status, github_app_installation_id,
                           github_app_installation_status, ci_log_group_arn, cd_log_group_arn, hash)
VALUES ('Xtages', 'cus_JNlr07Ilq8tnxc', 'ACTIVE', 16569285, 'ACTIVE',
        'arn:aws:logs:us-east-1:606626603369:log-group:xtages_ci_logs',
        'arn:aws:logs:us-east-1:606626603369:log-group:xtages_cd_logs', 'e374f5be2de51dbbd75cc52cc1badcf1');

-- czuniga@xtages.com
INSERT INTO "xtages_user"
    (id, cognito_user_id, organization_name, is_owner, cognito_identity_id)
VALUES (1, '5b2a09e0-ddd5-4450-bab1-f924725f247a', 'Xtages', true, 'us-east-1:6831d19d-ff68-453e-95a8-41ee218bf754');

SELECT pg_catalog.setval('public.xtages_user_id_seq', 1, true);

SELECT pg_catalog.setval('public.project_id_seq', 1, true);

INSERT INTO "stripe_checkout_session"
    (organization_name, stripe_checkout_session_id)
VALUES ('Xtages', 'cs_test_a1d6zBhwGHiJWtj6YhMvLiXqLybR5qTtNO8Pf5HVasQrieGf0H6QavPQB8');

INSERT INTO "recipe" (project_type, version, repository, tag, build_script_path, deploy_script_path,
                      promote_script_path, rollback_script_path)
VALUES ('NODE', '15.13.0', 'Xtages/recipes', 'v0.1.6', 'node/ci/build.sh', 'node/cd/deploy.sh',
        'node/cd/promote.sh', 'node/cd/rollback.sh');

INSERT INTO "organization_to_plan" (organization_name, plan_id, start_time, end_time)
    (SELECT 'Xtages', "id", now(), NULL FROM "plan" WHERE "name" = 'Unlimited');

INSERT INTO "plan" (name, limit_projects, limit_monthly_build_minutes, limit_monthly_data_transfer_gbs)
VALUES ('Tiny for testing', 1, 1, 1);

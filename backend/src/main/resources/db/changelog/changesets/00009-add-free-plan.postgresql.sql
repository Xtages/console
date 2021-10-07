-- liquibase formatted sql
-- changeset mdellamerlina:25 logicalFilePath:xtages-console.xml

INSERT INTO recipe(project_type, version, repository, tag, build_script_path, deploy_script_path,
 promote_script_path, rollback_script_path)
VALUES ('NODE', '15.13.0', 'Xtages/recipes', 'v0.1.21', 'node/ci/build.sh', 'node/cd/deploy.sh',
 'node/cd/promote.sh', 'node/cd/rollback.sh');

-- Starter annually plan
INSERT INTO public.plan(
    name,
    limit_projects,
    limit_monthly_build_minutes,
    limit_monthly_data_transfer_gbs,
    db_instance,
    db_storage_gbs,
    product_id)
VALUES
    ('Free',
     1,
     100,
     1,
     'db.t4g.micro',
     5,
     'N/A');

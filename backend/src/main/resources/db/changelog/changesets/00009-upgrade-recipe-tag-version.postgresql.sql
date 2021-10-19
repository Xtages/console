-- liquibase formatted sql
-- changeset mdellamerlina:28 logicalFilePath:xtages-console.xml

INSERT INTO recipe(project_type, version, repository, tag, build_script_path, deploy_script_path,
 promote_script_path, rollback_script_path)
VALUES ('NODE', '15.13.0', 'Xtages/recipes', 'v0.1.24', 'node/ci/build.sh', 'node/cd/deploy.sh',
 'node/cd/promote.sh', 'node/cd/rollback.sh');


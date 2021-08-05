-- liquibase formatted sql

-- changeset mdellamerlina:1 logicalFilePath:seed-xtages-console-dev-db.xml

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
    ('Starter annually',
     1,
     2500,
     20,
     'db.t3.medium',
     20,
     'prod_JlMOKm62S0nw4u');

-- Starter monthly plan
INSERT INTO public.plan(
    name,
    limit_projects,
    limit_monthly_build_minutes,
    limit_monthly_data_transfer_gbs,
    db_instance,
    db_storage_gbs,
    product_id)
VALUES
    ('Starter monthly',
     1,
     2500,
     20,
     'db.t3.medium',
     20,
     'prod_JlMSuNcAWvH93b');

-- Professional annually plan
INSERT INTO public.plan(
    name,
    limit_projects,
    limit_monthly_build_minutes,
    limit_monthly_data_transfer_gbs,
    db_instance,
    db_storage_gbs,
    product_id)
VALUES
    ('Professional annually',
     3,
     7500,
     500,
     'db.t3.medium',
     20,
     'prod_JlMTyIPa78LDcn');

-- Professional monthly plan
INSERT INTO public.plan(
    name,
    limit_projects,
    limit_monthly_build_minutes,
    limit_monthly_data_transfer_gbs,
    db_instance,
    db_storage_gbs,
    product_id)
VALUES
    ('Professional monthly',
     3,
     7500,
     500,
     'db.t3.medium',
     20,
     'prod_JlMUWhrUK2NTfc');

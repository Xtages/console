-- liquibase formatted sql


-- changeset czuniga:1
CREATE TABLE "xtages_user"
(
    "id"              SERIAL PRIMARY KEY,
    "cognito_user_id" VARCHAR(255) NOT NULL UNIQUE
);

-- changeset czuniga:2
CREATE TYPE "organization_subscription_status" AS ENUM
    ('UNCONFIRMED','ACTIVE','SUSPENDED','PENDING_CANCELLATION','CANCELLED');
CREATE TABLE "organization"
(
    "name"                VARCHAR(255) PRIMARY KEY,
    "stripe_customer_id"  VARCHAR(255),
    "subscription_status" "organization_subscription_status" NOT NULL,
    "owner_id"            INT                                NOT NULL REFERENCES "xtages_user" ("id")
);

-- changeset czuniga:3
CREATE TABLE "stripe_checkout_session"
(
    "organization_name"          VARCHAR(255) NOT NULL PRIMARY KEY REFERENCES "organization" ("name"),
    "stripe_checkout_session_id" VARCHAR(255) NOT NULL
)

